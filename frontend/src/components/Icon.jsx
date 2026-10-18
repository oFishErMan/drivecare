export function Icon({ name }) {
  const paths = {
    check: <path d="m5 12 4 4L19 6" />,
    close: <path d="m6 6 12 12M18 6 6 18" />,
    search: <><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 5 5" /></>,
    refresh: <><path d="M20 7v5h-5M4 17v-5h5" /><path d="M6 7a7 7 0 0 1 12-1l2 3M4 15l2 3a7 7 0 0 0 12-1" /></>,
    trash: (
      <>
        <path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7" />
      </>
    ),
    car: (
      <>
        <path d="M5 17h14l-1.3-5.2a2 2 0 0 0-1.94-1.52H8.24A2 2 0 0 0 6.3 11.8L5 17Z" />
        <path d="M3 17h18v2H3z" />
        <circle cx="7" cy="19" r="2" />
        <circle cx="17" cy="19" r="2" />
        <path d="m8 10 1.2-3h5.6l1.2 3" />
      </>
    ),
    oil: (
      <>
        <path d="M5 15h11l2-5-4-3H7L5 15Z" />
        <path d="M3 11h2M9 7V4h4v3M19 14c0-1 2-3 2-3s2 2 2 3a2 2 0 0 1-4 0Z" />
      </>
    ),
    filter: (
      <>
        <path d="M4 6h16M6 10h12M8 14h8M10 18h4" />
      </>
    ),
    tire: (
      <>
        <circle cx="12" cy="12" r="8" />
        <circle cx="12" cy="12" r="4" />
        <path d="M12 4v4M12 16v4M4 12h4M16 12h4" />
      </>
    ),
    battery: (
      <>
        <rect x="4" y="7" width="16" height="12" rx="2" />
        <path d="M8 7V4h3v3M15 7V4h3v3M8 13h4M10 11v4M15 13h2" />
      </>
    ),
    tool: (
      <>
        <path d="M14 6a4 4 0 0 0-5 5L4 16l4 4 5-5a4 4 0 0 0 5-5l-3 3-3-3 2-4Z" />
      </>
    ),
    gauge: (
      <>
        <path d="M4 17a8 8 0 1 1 16 0" />
        <path d="m12 14 4-4" />
        <path d="M7 17h10" />
      </>
    ),
    route: (
      <>
        <circle cx="6" cy="18" r="2" />
        <circle cx="18" cy="6" r="2" />
        <path d="M8 18h2a2 2 0 0 0 2-2V8a2 2 0 0 1 2-2h2" />
      </>
    ),
    calendar: (
      <>
        <rect x="4" y="5" width="16" height="15" rx="2" />
        <path d="M8 3v4M16 3v4M4 10h16" />
      </>
    ),
    bell: (
      <>
        <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
        <path d="M10 21h4" />
      </>
    ),
    plus: (
      <>
        <path d="M12 5v14M5 12h14" />
      </>
    ),
    arrow: (
      <>
        <path d="M5 12h14M14 7l5 5-5 5" />
      </>
    ),
    spark: (
      <>
        <path d="m12 3 1.4 4.1L17.5 8.5l-4.1 1.4L12 14l-1.4-4.1-4.1-1.4 4.1-1.4L12 3Z" />
        <path d="m19 14 .8 2.2L22 17l-2.2.8L19 20l-.8-2.2L16 17l2.2-.8L19 14Z" />
      </>
    ),
    info: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M12 11v5M12 8h.01" />
      </>
    ),
  }
  return (
    <svg
      className="icon"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[name] || paths.tool}
    </svg>
  )
}
