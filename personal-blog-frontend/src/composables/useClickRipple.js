export function mountClickRipple() {
  const prefersReducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches ?? false;

  function onPointerDown(e) {
    if (e.button !== 0) return;
    const target = e.target;
    if (!(target instanceof Element)) return;
    if (target.closest('input, textarea, select, [contenteditable="true"], button.menu-toggle')) return;

    const el = document.createElement('span');
    el.setAttribute('aria-hidden', 'true');
    const size = prefersReducedMotion ? 48 : 132;
    const half = size / 2;
    el.style.cssText = [
      'position:fixed',
      `left:${e.clientX}px`,
      `top:${e.clientY}px`,
      `width:${size}px`,
      `height:${size}px`,
      `margin-left:-${half}px`,
      `margin-top:-${half}px`,
      'border-radius:50%',
      'pointer-events:none',
      'z-index:12500',
      'background:radial-gradient(circle,rgba(37,99,235,0.38) 0%,rgba(37,99,235,0.12) 42%,transparent 72%)',
      'transform:scale(0.3)',
      `opacity:${prefersReducedMotion ? '0.45' : '1'}`,
      `transition:transform ${prefersReducedMotion ? '0.06s' : '0.52s'} cubic-bezier(0.22,1,0.36,1),opacity ${prefersReducedMotion ? '0.08s' : '0.48s'} ease-out`,
    ].join(';');

    document.body.appendChild(el);
    requestAnimationFrame(() => {
      el.style.transform = 'scale(2.35)';
      el.style.opacity = '0';
    });
    window.setTimeout(() => el.remove(), prefersReducedMotion ? 120 : 580);
  }

  document.addEventListener('pointerdown', onPointerDown, true);
  return () => document.removeEventListener('pointerdown', onPointerDown, true);
}
