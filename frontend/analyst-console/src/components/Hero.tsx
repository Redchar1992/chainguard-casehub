import { useI18n } from '../i18n';

/** Ambient, on-brand hero art: a shield (ChainGuard) over a chain of links, with a
 * radar sweep scanning across — the "watchdog scanning the chain" motif. Pure SVG +
 * SMIL/CSS (no canvas/JS), low-key, and stilled under prefers-reduced-motion. */
function HeroArt() {
  return (
    <svg viewBox="0 0 280 180" className="hero-art-svg" role="img" aria-hidden="true">
      <defs>
        <linearGradient id="cgLg" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#6aa0ff" />
          <stop offset="1" stopColor="#bcd4ff" />
        </linearGradient>
        <radialGradient id="cgGlow" cx="50%" cy="50%" r="50%">
          <stop offset="0" stopColor="#6aa0ff" stopOpacity="0.22" />
          <stop offset="1" stopColor="#6aa0ff" stopOpacity="0" />
        </radialGradient>
        <radialGradient id="cgScan" cx="0%" cy="0%" r="100%">
          <stop offset="0" stopColor="#6aa0ff" stopOpacity="0.32" />
          <stop offset="1" stopColor="#6aa0ff" stopOpacity="0" />
        </radialGradient>
        <path id="cgFlow" d="M40 150 H240" fill="none" />
        <clipPath id="cgRadarClip">
          <circle cx="140" cy="90" r="58" />
        </clipPath>
      </defs>

      <ellipse cx="140" cy="90" rx="124" ry="84" fill="url(#cgGlow)" />

      {/* radar field behind the shield */}
      <g clipPath="url(#cgRadarClip)">
        <circle cx="140" cy="90" r="58" fill="none" stroke="#232a36" strokeWidth="1" />
        <circle cx="140" cy="90" r="38" fill="none" stroke="#232a36" strokeWidth="1" />
        <circle cx="140" cy="90" r="18" fill="none" stroke="#232a36" strokeWidth="1" />
        <g className="cg-scan">
          <path d="M140 90 L140 32 A58 58 0 0 1 198 90 Z" fill="url(#cgScan)" />
          <line x1="140" y1="90" x2="140" y2="32" stroke="#6aa0ff" strokeWidth="1.2" strokeOpacity="0.7" />
        </g>
      </g>

      {/* shield */}
      <g className="cg-shield">
        <path
          d="M140 44 L176 56 V92 C176 116 160 130 140 138 C120 130 104 116 104 92 V56 Z"
          fill="none"
          stroke="url(#cgLg)"
          strokeWidth="3.2"
          strokeLinejoin="round"
        />
        <path d="M126 90 L137 101 L156 78" fill="none" stroke="url(#cgLg)" strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round" />
      </g>

      {/* chain along the bottom */}
      <g fill="none" stroke="url(#cgLg)" strokeWidth="3" strokeOpacity="0.85">
        <rect x="56" y="138" width="40" height="24" rx="12" />
        <rect x="90" y="138" width="40" height="24" rx="12" />
        <rect x="124" y="138" width="40" height="24" rx="12" />
        <rect x="158" y="138" width="40" height="24" rx="12" />
        <rect x="192" y="138" width="40" height="24" rx="12" />
      </g>

      {/* light moving along the chain */}
      <g className="flowdots">
        <circle r="3" fill="#e8f0ff">
          <animateMotion dur="3.6s" repeatCount="indefinite">
            <mpath href="#cgFlow" />
          </animateMotion>
        </circle>
        <circle r="2.4" fill="#9bc0ff">
          <animateMotion dur="3.6s" begin="1.2s" repeatCount="indefinite">
            <mpath href="#cgFlow" />
          </animateMotion>
        </circle>
        <circle r="2" fill="#6aa0ff">
          <animateMotion dur="3.6s" begin="2.4s" repeatCount="indefinite">
            <mpath href="#cgFlow" />
          </animateMotion>
        </circle>
      </g>
    </svg>
  );
}

export function Hero() {
  const { t } = useI18n();
  return (
    <div className="hero">
      <div className="hero-glow" aria-hidden />
      <div className="hero-text">
        <h1>{t('hero.headline')}</h1>
        <p>{t('hero.sub')}</p>
        <div className="namecard">
          <span className="term">ChainGuard CaseHub</span>
          <span className="def">{t('hero.def')}</span>
        </div>
        <div>
          <a className="cta" href="#workbench">{t('hero.cta')} →</a>
        </div>
      </div>
      <div className="hero-art">
        <HeroArt />
      </div>
    </div>
  );
}

export function HowItWorks() {
  const { t } = useI18n();
  const steps = [
    { title: t('how.s1t'), desc: t('how.s1d') },
    { title: t('how.s2t'), desc: t('how.s2d') },
    { title: t('how.s3t'), desc: t('how.s3d') },
    { title: t('how.s4t'), desc: t('how.s4d') },
  ];
  return (
    <div className="how">
      <div className="section-title">{t('how.title')}</div>
      <div className="steps">
        {steps.map((s, i) => (
          <a className="step" key={i} href="#workbench">
            <div className="step-num">{i + 1}</div>
            <div className="step-title">{s.title}</div>
            <div className="step-desc">{s.desc}</div>
            <span className="step-cue">{t('how.go')}</span>
          </a>
        ))}
      </div>
    </div>
  );
}
