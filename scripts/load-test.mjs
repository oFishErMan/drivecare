// Local-only API load test. Keeps identifiable test accounts for inspection.
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { mkdirSync, writeFileSync } from 'node:fs'
import { performance } from 'node:perf_hooks'
import path from 'node:path'

const base = 'http://localhost:3000'
const users = 100
const durationMs = Number(process.env.LOAD_DURATION_SECONDS || 45) * 1000
const runId = new Date().toISOString().replace(/[^0-9]/g, '').slice(0,14)
const prefix = `loadtest-${runId}`
const password = 'LoadTest1234!'
const today = new Intl.DateTimeFormat('en-CA', { timeZone:'Asia/Seoul', year:'numeric', month:'2-digit', day:'2-digit' }).format(new Date())
const results = { runId, base, users, durationSeconds:durationMs/1000, prefix, date:today, checks:[], phases:{}, failures:[] }
let phase = 'seed', active = 0, peak = 0
const samples = []
const sleep = (ms) => new Promise(resolve => setTimeout(resolve,ms))
function sql(query) {
  return execFileSync('docker', ['compose','exec','-T','mariadb','sh','-c','mariadb -u"$MARIADB_USER" -p"$MARIADB_PASSWORD" "$MARIADB_DATABASE" -N -B -e "$1"','sql',query], {cwd:path.resolve('C:/Spring/_Project/drivecare'),encoding:'utf8'}).trim()
}
function check(name, action) {
  try { action(); results.checks.push({name,passed:true}) }
  catch(error) { results.checks.push({name,passed:false,detail:error.message}); console.log(`FAIL: ${name}: ${error.message}`) }
}
function addDays(date, days) { const d = new Date(`${date}T12:00:00Z`); d.setUTCDate(d.getUTCDate()+days); return d.toISOString().slice(0,10) }
function addMonths(date, months) { const d=new Date(`${date}T12:00:00Z`); const day=d.getUTCDate(); d.setUTCDate(1); d.setUTCMonth(d.getUTCMonth()+months); const last=new Date(Date.UTC(d.getUTCFullYear(),d.getUTCMonth()+1,0)).getUTCDate(); d.setUTCDate(Math.min(day,last)); return d.toISOString().slice(0,10) }
class Client {
  cookies = new Map()
  token = null
  lastStatus = 0
  async request(url, method='GET', body, expected=200) {
    const started=performance.now(); let status=0
    active++; peak=Math.max(peak,active)
    try {
      const headers={ Cookie:[...this.cookies].map(([key,value])=>`${key}=${value}`).join(';') }
      if(body !== undefined) headers['Content-Type']='application/json'
      if(method !== 'GET' && this.token) headers['X-XSRF-TOKEN']=this.token
      const response=await fetch(base+url,{method,headers,body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(30000)})
      status=response.status
      this.lastStatus=status
      for(const cookie of response.headers.getSetCookie()) { const [pair]=cookie.split(';'); const index=pair.indexOf('='); this.cookies.set(pair.slice(0,index),pair.slice(index+1)) }
      const text=await response.text(); let data
      try { data=JSON.parse(text) } catch { data=text }
      if(!(Array.isArray(expected)?expected:[expected]).includes(status)) throw new Error(`${method} ${url}: expected ${expected}, got ${status}: ${text.slice(0,180)}`)
      return data
    } finally { active--; samples.push({phase,url,method,status,ms:performance.now()-started}) }
  }
  async csrf() { this.token=(await this.request('/api/auth/csrf')).token }
  async login(email) { await this.csrf(); const me=await this.request('/api/auth/login','POST',{email,password}); await this.csrf(); return me }
}
const fixtures = [
  {name:'정상',km:4000,months:12,date:today,state:'GOOD'},
  {name:'임박직전',km:1001,months:12,date:today,state:'GOOD'},
  {name:'임박경계',km:1000,months:12,date:today,state:'DUE_SOON'},
  {name:'교체도달',km:0,months:12,date:today,state:'OVERDUE'},
  {name:'주기초과',km:-5100,months:12,date:today,state:'OVERDUE'},
  {name:'기간도달',km:null,months:12,date:addMonths(today,-12),state:'OVERDUE'},
  {name:'기간임박',km:null,months:12,date:addMonths(addDays(today,30),-12),state:'DUE_SOON'},
]
async function pool(values, concurrency, action) {
  let index=0
  await Promise.all(Array.from({length:Math.min(concurrency,values.length)},async()=>{while(index<values.length) {const i=index++; await action(values[i],i)}}))
}
const accounts=[]
const demoSql="SELECT u.id,u.email,v.id,v.current_odometer_km,m.id,m.last_replaced_odometer_km,m.interval_km FROM app_users u LEFT JOIN vehicles v ON v.owner_id=u.id LEFT JOIN maintenance_items m ON m.vehicle_id=v.id WHERE u.email='test@drivecare.local' ORDER BY v.id,m.id"
const demoBefore=sql(demoSql)
const startedAt=new Date().toISOString()
console.log(`Seeding ${users} accounts, ${users} vehicles, ${users*fixtures.length} maintenance items (${prefix})`)
try {
  await pool(Array.from({length:users},(_,i)=>i),10,async i=>{
    const client=new Client(),email=`${prefix}-${String(i+1).padStart(3,'0')}@example.test`
    await client.csrf()
    await client.request('/api/auth/register','POST',{email,password,displayName:`부하 테스트 ${i+1}`},201)
    await client.login(email)
    const vehicle=await client.request('/api/vehicles','POST',{manufacturer:'테스트 현대',model:`정합성 차량 ${i+1}`,modelYear:2023,currentOdometerKm:120000,monthlyAverageKm:1500},201)
    const expected=[]
    for(const fixture of fixtures) {
      const item=await client.request('/api/maintenance-items','POST',{vehicleId:vehicle.id,category:fixture.km===null?'BATTERY':'ENGINE_OIL',productName:`${prefix} ${fixture.name}`,lastReplacedOn:fixture.date,lastReplacedOdometerKm:fixture.km===null?120000:112000+fixture.km,intervalKm:fixture.km===null?null:8000,intervalMonths:fixture.months},201)
      expected.push({id:item.id,fixture})
      assert.equal(item.status.state,fixture.state,fixture.name)
      assert.equal(item.status.remainingKm,fixture.km,fixture.name)
      assert.equal(item.status.nextDate,addMonths(fixture.date,fixture.months))
    }
    accounts[i]={client,email,vehicle,expected}
  })
  check('100명 데이터 생성과 700개 소모품 거리·기간 경계 계산',()=>assert.equal(accounts.filter(Boolean).length,100))
  phase='consistency'
  const a=accounts[0], b=accounts[1]
  await a.client.request(`/api/vehicles/${b.vehicle.id}/odometer`,'POST',{recordedOn:today,odometerKm:120001},404)
  await a.client.request('/api/maintenance-items','POST',{vehicleId:b.vehicle.id,category:'ENGINE_OIL',lastReplacedOn:today,lastReplacedOdometerKm:100000,intervalKm:8000},404)
  results.checks.push({name:'다른 사용자의 차량 수정·소모품 등록 차단',passed:true})
  await a.client.request(`/api/vehicles/${a.vehicle.id}/odometer`,'POST',{recordedOn:today,odometerKm:119999},400)
  await a.client.request('/api/vehicles','POST',{manufacturer:'테스트',model:'음수',currentOdometerKm:-1},400)
  await a.client.request('/api/maintenance-items','POST',{vehicleId:a.vehicle.id,category:'ENGINE_OIL',lastReplacedOn:today,lastReplacedOdometerKm:130000,intervalKm:8000},400)
  await a.client.request('/api/maintenance-items','POST',{vehicleId:a.vehicle.id,category:'ENGINE_OIL',lastReplacedOn:today,lastReplacedOdometerKm:100000},400)
  results.checks.push({name:'주행거리 감소·음수·현재보다 큰 교체거리·누락 주기 차단',passed:true})
  // Race: the largest reading must survive, lower readings may be rejected.
  await Promise.all(Array.from({length:100},(_,i)=>a.client.request(`/api/vehicles/${a.vehicle.id}/odometer`,'POST',{recordedOn:today,odometerKm:120001+i},[200,400])))
  const raced=(await a.client.request('/api/vehicles')).find(v=>v.id===a.vehicle.id)
  check('동일 차량 100개 동시 수정 후 최댓값 보존',()=>assert.equal(raced.currentOdometerKm,120100))
  check('동일 차량·날짜 주행기록 중복 없음',()=>assert.equal(sql(`SELECT COUNT(*) FROM odometer_entries WHERE vehicle_id=${a.vehicle.id} AND recorded_on='${today}'`),'1'))
  a.vehicle.currentOdometerKm=raced.currentOdometerKm
  await a.client.request('/api/maintenance-items','POST',{vehicleId:a.vehicle.id,category:'OTHER',productName:`${prefix} 미래일 검증`,lastReplacedOn:addDays(today,1),lastReplacedOdometerKm:120000,intervalMonths:12},[201,400])
  check('API에서 미래 교체일 차단',()=>assert.equal(a.client.lastStatus,400,'미래 교체일을 서버가 허용함 (201)'))
  console.log('100 independent users: simultaneous login burst')
  phase='login-burst'; peak=0
  const loginStart=performance.now()
  await Promise.all(accounts.map(async a=>{a.client=new Client(); const me=await a.client.login(a.email); assert.equal(me.email,a.email)}))
  results.loginBurst={elapsedSeconds:(performance.now()-loginStart)/1000,peakRequests:peak}
  console.log(`100 active users: mixed read/write for ${durationMs/1000}s`)
  phase='load'; peak=0
  const loadStart=performance.now(),end=loadStart+durationMs
  await Promise.all(accounts.map(async a=>{
    let round=0,odometer=a.vehicle.currentOdometerKm
    while(performance.now()<end) {
      try {
        const me=await a.client.request('/api/auth/me'); assert.equal(me.email,a.email)
        const vehicles=await a.client.request('/api/vehicles'); assert.equal(vehicles.length,1); assert.equal(vehicles[0].id,a.vehicle.id); assert.equal(vehicles[0].currentOdometerKm,odometer)
        const items=await a.client.request('/api/maintenance-items'); assert.ok(items.every(item=>item.vehicleId===a.vehicle.id))
        for(const entry of a.expected) { const item=items.find(v=>v.id===entry.id); assert.ok(item); assert.equal(item.status.remainingKm,entry.fixture.km===null?null:120000+entry.fixture.km-odometer) }
        const notifications=await a.client.request('/api/notifications'); assert.ok(Array.isArray(notifications))
        if(++round%5===0) { odometer++; await a.client.request(`/api/vehicles/${a.vehicle.id}/odometer`,'POST',{recordedOn:today,odometerKm:odometer}) }
      } catch(error) { results.failures.push({phase,email:a.email,message:error.message}); await sleep(1000) }
      await sleep(200)
    }
  }))
  results.load={elapsedSeconds:(performance.now()-loadStart)/1000,peakRequests:peak}
  phase='postcheck'
  check('기존 데모 데이터 유지',()=>assert.equal(sql(demoSql),demoBefore))
  const dbQueries={
    testAccounts:`SELECT COUNT(*) FROM app_users WHERE email LIKE '${prefix}-%'`,
    testVehicles:`SELECT COUNT(*) FROM vehicles v JOIN app_users u ON u.id=v.owner_id WHERE u.email LIKE '${prefix}-%'`,
    testItems:`SELECT COUNT(*) FROM maintenance_items m JOIN vehicles v ON v.id=m.vehicle_id JOIN app_users u ON u.id=v.owner_id WHERE u.email LIKE '${prefix}-%'`,
    orphanVehicles:'SELECT COUNT(*) FROM vehicles v LEFT JOIN app_users u ON u.id=v.owner_id WHERE u.id IS NULL',
    orphanItems:'SELECT COUNT(*) FROM maintenance_items m LEFT JOIN vehicles v ON v.id=m.vehicle_id WHERE v.id IS NULL',
    duplicateEmails:'SELECT COUNT(*) FROM (SELECT email FROM app_users GROUP BY email HAVING COUNT(*)>1) t',
    duplicateOdometers:'SELECT COUNT(*) FROM (SELECT vehicle_id,recorded_on FROM odometer_entries GROUP BY vehicle_id,recorded_on HAVING COUNT(*)>1) t',
    duplicateNotifications:'SELECT COUNT(*) FROM (SELECT dedupe_key FROM notifications GROUP BY dedupe_key HAVING COUNT(*)>1) t',
    odometerMismatch:'SELECT COUNT(*) FROM vehicles v LEFT JOIN (SELECT vehicle_id,MAX(odometer_km) km FROM odometer_entries GROUP BY vehicle_id) o ON o.vehicle_id=v.id WHERE o.km IS NULL OR o.km<>v.current_odometer_km',
    negativeDistances:'SELECT COUNT(*) FROM vehicles WHERE current_odometer_km<0',
    incorrectReplacement:'SELECT COUNT(*) FROM maintenance_items m JOIN vehicles v ON v.id=m.vehicle_id WHERE m.last_replaced_odometer_km>v.current_odometer_km',
    notificationOwnerMismatch:'SELECT COUNT(*) FROM notifications n JOIN maintenance_items m ON m.id=n.maintenance_item_id JOIN vehicles v ON v.id=m.vehicle_id WHERE n.user_id<>v.owner_id',
  }
  results.database=Object.fromEntries(Object.entries(dbQueries).map(([name,query])=>[name,Number(sql(query))]))
  for(const [name,value] of Object.entries(results.database)) if(!name.startsWith('test')) check(`DB ${name}`,()=>assert.equal(value,0))
  check('모든 테스트 세션의 사용자별 차량·소모품 격리 및 저장값 조회',()=>assert.equal(results.failures.length,0))
} catch(error) { results.failures.push({phase,message:error.stack}); console.error(error.message) }
finally {
  const percentile=(values,p)=>values[Math.max(0,Math.ceil(values.length*p)-1)]||0
  for(const p of [...new Set(samples.map(s=>s.phase))]) {
    const rows=samples.filter(s=>s.phase===p),times=rows.map(s=>s.ms).sort((a,b)=>a-b)
    results.phases[p]={requests:rows.length,statuses:rows.reduce((a,s)=>(a[s.status]=(a[s.status]||0)+1,a),{}),p50Ms:percentile(times,.5),p95Ms:percentile(times,.95),p99Ms:percentile(times,.99),maxMs:times.at(-1)}
  }
  const loadRows=samples.filter(s=>s.phase==='load')
  results.endpointMetrics=Object.fromEntries([...new Set(loadRows.map(s=>`${s.method} ${s.url.replace(/\/\d+\//g,'/:id/')}`))].map(key=>{const rows=loadRows.filter(s=>`${s.method} ${s.url.replace(/\/\d+\//g,'/:id/')}`===key);const times=rows.map(s=>s.ms).sort((a,b)=>a-b);return[key,{requests:rows.length,p95Ms:percentile(times,.95),errors:rows.filter(s=>s.status!==200).length}]}))
  if(results.load) results.load.requestsPerSecond=loadRows.length/results.load.elapsedSeconds
  results.startedAt=startedAt; results.finishedAt=new Date().toISOString()
  const dir='D:/DriveCareTestResults'; mkdirSync(dir,{recursive:true})
  const report=path.join(dir,`${prefix}.json`);writeFileSync(report,JSON.stringify(results,null,2))
  console.log(JSON.stringify({report,phases:results.phases,loginBurst:results.loginBurst,load:results.load,database:results.database,failedChecks:results.checks.filter(c=>!c.passed),failures:results.failures.slice(0,5)},null,2))
  if(results.failures.length || results.checks.some(c=>!c.passed)) process.exitCode=1
}
