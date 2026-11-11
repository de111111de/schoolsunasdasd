/* SCHOOLSUN — общие анимации интерфейса (тема, reveal, тосты, конфетти, параллакс).
   Движение построено на GSAP + ScrollTrigger; без них (или при prefers-reduced-motion)
   включается запасной вариант на CSS и IntersectionObserver. */
(function () {
  'use strict';

  var reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var finePointer = window.matchMedia && window.matchMedia('(pointer: fine)').matches;
  var root = document.documentElement;
  var gsap = window.gsap;
  var hasGsap = !!gsap && !reduce;
  var ST = hasGsap ? window.ScrollTrigger : null;
  if (ST) gsap.registerPlugin(ST);
  root.classList.add('ss-ready', hasGsap ? 'ss-gsap' : 'ss-static');

  function each(sel, fn, ctx) { Array.prototype.forEach.call((ctx || document).querySelectorAll(sel), fn); }

  /* ---------- тема: свет «рассвета» / ночь со звёздами ---------- */
  try {
    if (localStorage.getItem('ss-theme') === 'night') {
      document.documentElement.setAttribute('data-night', '');
    }
  } catch (e) { /* приватный режим — просто остаёмся в светлой теме */ }

  document.addEventListener('DOMContentLoaded', function () {
    var themeBtn = document.getElementById('ss-theme');
    if (themeBtn) {
      themeBtn.setAttribute('aria-pressed', root.hasAttribute('data-night') ? 'true' : 'false');
      var flipTheme = function () {
        var night = root.toggleAttribute('data-night');
        themeBtn.setAttribute('aria-pressed', night ? 'true' : 'false');
        try { localStorage.setItem('ss-theme', night ? 'night' : 'day'); } catch (e) {}
      };
      themeBtn.addEventListener('click', function () {
        if (reduce || !document.startViewTransition) { flipTheme(); return; }
        /* новая тема раскрывается кругом от кнопки */
        var r = themeBtn.getBoundingClientRect();
        root.style.setProperty('--vt-x', (r.left + r.width / 2) + 'px');
        root.style.setProperty('--vt-y', (r.top + r.height / 2) + 'px');
        root.classList.add('ss-vt');
        var done = function () { root.classList.remove('ss-vt'); };
        try {
          document.startViewTransition(flipTheme).finished.then(done, done);
        } catch (e) { done(); flipTheme(); }
      });
    }

    /* ---------- шапка: тень после прокрутки и индикатор чтения ---------- */
    var header = document.querySelector('.ss-header');
    var progress = header ? header.querySelector('.ss-header__progress') : null;
    if (header) {
      var onScroll = function () {
        var y = window.pageYOffset || 0;
        header.classList.toggle('is-scrolled', y > 8);
        if (progress) {
          var max = root.scrollHeight - window.innerHeight;
          progress.style.transform = 'scaleX(' + (max > 40 ? Math.min(y / max, 1) : 0).toFixed(4) + ')';
        }
      };
      window.addEventListener('scroll', onScroll, { passive: true });
      onScroll();
    }

    /* ---------- бургер-меню ---------- */
    var burger = document.getElementById('ss-burger');
    var nav = document.getElementById('ss-nav');
    if (burger && nav) {
      burger.addEventListener('click', function () {
        var open = burger.classList.toggle('open');
        nav.classList.toggle('open', open);
      });
      nav.addEventListener('click', function (e) {
        if (e.target.tagName === 'A') {
          burger.classList.remove('open');
          nav.classList.remove('open');
        }
      });
    }

    /* ---------- звёзды в hero (только тёмная тема) ---------- */
    var stars = document.getElementById('ss-stars');
    if (stars) {
      var sf = document.createDocumentFragment();
      for (var i = 0; i < 70; i++) {
        var s = document.createElement('i');
        s.style.left = (Math.random() * 100).toFixed(2) + '%';
        s.style.top = (Math.random() * 100).toFixed(2) + '%';
        s.style.animationDuration = (1 + Math.random() * 2.4).toFixed(2) + 's';
        s.style.animationDelay = (Math.random() * 3).toFixed(2) + 's';
        if (Math.random() < .2) { s.style.width = '3px'; s.style.height = '3px'; }
        sf.appendChild(s);
      }
      stars.appendChild(sf);
    }

    /* ---------- парящие частицы в hero ---------- */
    var parts = document.getElementById('ss-particles');
    if (parts && !reduce) {
      var pf = document.createDocumentFragment();
      for (var j = 0; j < 16; j++) {
        var p = document.createElement('span');
        p.style.left = (4 + Math.random() * 92).toFixed(1) + '%';
        var size = (3 + Math.random() * 4).toFixed(1);
        p.style.width = size + 'px';
        p.style.height = size + 'px';
        p.style.animationDuration = (8 + Math.random() * 9).toFixed(1) + 's';
        p.style.animationDelay = (Math.random() * 10).toFixed(1) + 's';
        pf.appendChild(p);
      }
      parts.appendChild(pf);
    }

    /* ---------- ripple на кнопках ---------- */
    document.addEventListener('click', function (e) {
      var t = e.target;
      var b = t && t.closest ? t.closest('.ss-btn') : null;
      if (!b || reduce) return;
      var r = b.getBoundingClientRect();
      var size = Math.max(r.width, r.height);
      var span = document.createElement('span');
      span.className = 'ss-ripple';
      span.style.width = size + 'px';
      span.style.height = size + 'px';
      span.style.left = (e.clientX - r.left - size / 2) + 'px';
      span.style.top = (e.clientY - r.top - size / 2) + 'px';
      b.appendChild(span);
      setTimeout(function () { span.remove(); }, 650);
    });

    /* ---------- hero: восход солнца (GSAP-таймлайн + параллакс) ---------- */
    var hero = document.getElementById('ss-hero');
    if (hero && hasGsap) {
      var sun = document.getElementById('ss-hero-sun');
      var floats = hero.querySelectorAll('.ss-float__in');
      var tl = gsap.timeline({ defaults: { ease: 'expo.out' } });
      tl.fromTo(sun, { y: 300 }, { y: 0, duration: 2.4, ease: 'power3.out' }, 0)
        .to(hero.querySelector('.ss-hero__dusk'), { opacity: 0, duration: 2.6, ease: 'power2.out' }, 0)
        .to(hero.querySelectorAll('.ss-line > span'), { y: 0, yPercent: 0, duration: 1.25, stagger: .1 }, .2)
        .fromTo(hero.querySelectorAll('[data-intro]'), { opacity: 0, y: 22 },
          { opacity: 1, y: 0, duration: 1, stagger: .09, ease: 'power3.out' }, .55)
        .fromTo(floats, { opacity: 0, y: 46, scale: .9 },
          { opacity: 1, y: 0, scale: 1, duration: 1.1, stagger: .14, ease: 'back.out(1.5)' }, 1)
        .from(hero.querySelector('.ss-float__bar i'), { scaleX: 0, duration: 1.4, ease: 'power3.out' }, 1.5)
        .from(hero.querySelector('.ss-float__ring circle:last-child'),
          { strokeDashoffset: 113.1, duration: 1.4, ease: 'power3.out' }, 1.65);
      var cnt = hero.querySelector('[data-count]');
      if (cnt) {
        var cv = { v: 0 }, cmax = parseInt(cnt.getAttribute('data-count'), 10) || 0;
        tl.to(cv, { v: cmax, duration: 1.4, ease: 'power2.out',
          onUpdate: function () { cnt.textContent = Math.round(cv.v); } }, 1.8);
      }
      /* после появления карточки мягко «дышат» */
      tl.add(function () {
        Array.prototype.forEach.call(floats, function (el, i) {
          gsap.to(el, { y: i % 2 ? 9 : -11, duration: 3.2 + i * .7, ease: 'sine.inOut', yoyo: true, repeat: -1 });
        });
      });

      if (ST) {
        /* при прокрутке солнце продолжает подниматься, текст уходит, холмы отстают */
        var scrub = function () {
          return { trigger: hero, start: 0, end: function () { return hero.offsetHeight; }, scrub: .6 };
        };
        gsap.to('#ss-hero-orbit', { y: -110, ease: 'none', scrollTrigger: scrub() });
        gsap.to('#ss-hero-cards', { y: -50, ease: 'none', scrollTrigger: scrub() });
        gsap.to('#ss-hero-content', { y: 70, opacity: .1, ease: 'none', scrollTrigger: scrub() });
        gsap.to('#ss-hills-back', { yPercent: 28, ease: 'none', scrollTrigger: scrub() });
      }

      if (finePointer) {
        /* параллакс от курсора: у каждого слоя своя «глубина» */
        var layers = [];
        each('.ss-float', function (el) {
          layers.push({
            d: parseFloat(el.getAttribute('data-depth')) || 20,
            x: gsap.quickTo(el, 'x', { duration: .9, ease: 'power3.out' }),
            y: gsap.quickTo(el, 'y', { duration: .9, ease: 'power3.out' })
          });
        }, hero);
        var sunX = gsap.quickTo(sun, 'x', { duration: 1.2, ease: 'power3.out' });
        hero.addEventListener('mousemove', function (e) {
          var r = hero.getBoundingClientRect();
          var dx = (e.clientX - r.left) / r.width - .5;
          var dy = (e.clientY - r.top) / r.height - .5;
          for (var i = 0; i < layers.length; i++) { layers[i].x(dx * layers[i].d); layers[i].y(dy * layers[i].d); }
          sunX(dx * -18);
        });
        hero.addEventListener('mouseleave', function () {
          for (var i = 0; i < layers.length; i++) { layers[i].x(0); layers[i].y(0); }
          sunX(0);
        });
      }
    }

    /* ---------- бегущая строка предметов: ускоряется вместе со скроллом ---------- */
    var marquee = document.getElementById('ss-marquee');
    if (marquee) {
      marquee.innerHTML += marquee.innerHTML;
      if (hasGsap) {
        var run = gsap.to(marquee, { xPercent: -50, duration: 44, ease: 'none', repeat: -1 });
        if (ST) {
          ST.create({
            onUpdate: function (self) {
              run.timeScale(1 + Math.min(Math.abs(self.getVelocity()) / 260, 6));
              gsap.to(run, { timeScale: 1, duration: 1.1, delay: .08, ease: 'power2.out', overwrite: true });
            }
          });
        }
      }
    }

    /* ---------- солнце в финальном блоке поднимается по мере прокрутки ---------- */
    var ctaSun = document.getElementById('ss-cta-sun');
    if (ctaSun && ST) {
      gsap.fromTo(ctaSun, { yPercent: 34 }, {
        yPercent: 0, ease: 'none',
        scrollTrigger: { trigger: '#ss-cta', start: 'top bottom', end: 'bottom 55%', scrub: .8 }
      });
    }

    /* ---------- «магнитные» кнопки ---------- */
    if (hasGsap && finePointer) {
      each('[data-magnetic]', function (b) {
        b.classList.add('ss-magnetic');
        var mx = gsap.quickTo(b, 'x', { duration: .6, ease: 'power3.out' });
        var my = gsap.quickTo(b, 'y', { duration: .6, ease: 'power3.out' });
        b.addEventListener('mousemove', function (e) {
          var r = b.getBoundingClientRect();
          mx((e.clientX - r.left - r.width / 2) * .3);
          my((e.clientY - r.top - r.height / 2) * .45);
        });
        b.addEventListener('mouseleave', function () { mx(0); my(0); });
      });
    }

    /* ---------- карточки курсов: подсветка за курсором и 3D-наклон ---------- */
    if (finePointer) {
      each('.ss-course', function (card) {
        var rx = null, ry = null, lift = null;
        card.addEventListener('mouseenter', function () {
          if (!hasGsap || rx) return;
          /* ждём, пока закончится анимация появления, чтобы не спорить с ней за transform */
          if (card.classList.contains('ss-reveal') && !card.classList.contains('in')) return;
          gsap.set(card, { transformPerspective: 900 });
          rx = gsap.quickTo(card, 'rotationX', { duration: .6, ease: 'power3.out' });
          ry = gsap.quickTo(card, 'rotationY', { duration: .6, ease: 'power3.out' });
          lift = gsap.quickTo(card, 'y', { duration: .6, ease: 'power3.out' });
        });
        card.addEventListener('mousemove', function (e) {
          var r = card.getBoundingClientRect();
          card.style.setProperty('--mx', (e.clientX - r.left).toFixed(0) + 'px');
          card.style.setProperty('--my', (e.clientY - r.top).toFixed(0) + 'px');
          if (!rx) return;
          rx(-((e.clientY - r.top) / r.height - .5) * 6);
          ry(((e.clientX - r.left) / r.width - .5) * 6);
          lift(-5);
        });
        card.addEventListener('mouseleave', function () {
          if (!rx) return;
          rx(0); ry(0); lift(0);
        });
      });
    }

    /* ---------- выбор варианта в тесте ---------- */
    Array.prototype.forEach.call(document.querySelectorAll('.ss-opt input'), function (inp) {
      var sync = function () {
        var box = inp.closest('.ss-opt');
        if (inp.type === 'radio') {
          var name = inp.name;
          Array.prototype.forEach.call(document.querySelectorAll('input[name="' + name + '"]'), function (o) {
            o.closest('.ss-opt').classList.toggle('is-checked', o.checked);
          });
        } else {
          box.classList.toggle('is-checked', inp.checked);
        }
      };
      inp.addEventListener('change', sync);
      sync();
    });

    /* ---------- reveal, счётчики, прогресс-бары, кольцо ---------- */
    function fmt(n) { return String(n).replace(/\B(?=(\d{3})+(?!\d))/g, '\u2009'); }

    function animateCounter(el) {
      var target = parseInt(el.getAttribute('data-target'), 10) || 0;
      var suffix = el.getAttribute('data-suffix') || '';
      if (reduce) { el.textContent = fmt(target) + suffix; return; }
      if (hasGsap) {
        var o = { v: 0 };
        gsap.to(o, { v: target, duration: 1.8, ease: 'power3.out',
          onUpdate: function () { el.textContent = fmt(Math.round(o.v)) + suffix; } });
        return;
      }
      var t0 = null;
      function step(t) {
        if (t0 === null) t0 = t;
        var k = Math.min((t - t0) / 1400, 1);
        k = 1 - Math.pow(1 - k, 3);
        el.textContent = fmt(Math.round(target * k)) + suffix;
        if (k < 1) requestAnimationFrame(step);
      }
      requestAnimationFrame(step);
    }

    /* всё, что «оживает», когда блок попадает в экран */
    function activate(el) {
      each('[data-target]', function (c) {
        if (!c.getAttribute('data-done')) {
          c.setAttribute('data-done', '1');
          animateCounter(c);
        }
      }, el);
      each('.ss-progress i[data-val]', function (bar) {
        bar.style.width = bar.getAttribute('data-val') + '%';
      }, el);
      each('.ss-ring__val', function (ring) {
        var C = 364.4;
        var v = parseFloat(ring.getAttribute('data-val')) || 0;
        ring.style.strokeDashoffset = (C * (1 - v / 100)).toFixed(1);
      }, el);
      if (hasGsap) {
        var pops = el.querySelectorAll('[data-pop]');
        if (pops.length) {
          gsap.from(pops, { opacity: 0, y: 16, duration: .8, ease: 'power3.out', stagger: .09, delay: .25,
            clearProps: 'opacity,transform' });
        }
      }
    }

    each('.ss-reveal', function (el) {
      var ring = el.querySelector('.ss-ring__val');
      if (ring && !ring.getAttribute('data-val')) ring.setAttribute('data-val', '72');
    });

    if (ST) {
      /* блоки, вошедшие в экран одновременно, появляются каскадом */
      ST.batch('.ss-reveal', {
        start: 'top bottom-=60',
        once: true,
        onEnter: function (batch) {
          gsap.to(batch, {
            opacity: 1, y: 0, duration: 1, ease: 'power3.out', stagger: .08, overwrite: true,
            onComplete: function () {
              batch.forEach(function (el) { el.classList.add('in'); });
              gsap.set(batch, { clearProps: 'opacity,transform' });
            }
          });
          batch.forEach(activate);
        }
      });
    } else if ('IntersectionObserver' in window) {
      var io = new IntersectionObserver(function (entries) {
        entries.forEach(function (en) {
          if (!en.isIntersecting) return;
          en.target.classList.add('in');
          activate(en.target);
          io.unobserve(en.target);
        });
      }, { threshold: .15 });
      each('.ss-reveal', function (el) { io.observe(el); });
    } else {
      each('.ss-reveal', function (el) { el.classList.add('in'); activate(el); });
    }

    /* ---------- иконки Lucide ---------- */
    /* скрипт иконок грузится асинхронно и не задерживает страницу */
    var drawIcons = function () {
      if (window.lucide) window.lucide.createIcons({ attrs: { width: 18, height: 18 } });
    };
    drawIcons();
    var lucideTag = document.getElementById('ss-lucide');
    if (lucideTag && !window.lucide) lucideTag.addEventListener('load', drawIcons);

    /* ---------- тосты ---------- */
    var toasts = document.getElementById('ss-toasts');
    function showToast(text, isError) {
      if (!toasts || !text) return;
      var t = document.createElement('div');
      t.className = 'ss-toast' + (isError ? ' ss-toast--error' : '');
      t.innerHTML = '<i data-lucide="' + (isError ? 'alert-circle' : 'check-circle-2') + '" aria-hidden="true"></i><div>' + text + '</div>';
      toasts.appendChild(t);
      drawIcons();
      setTimeout(function () {
        t.classList.add('ss-toast--out');
        setTimeout(function () { t.remove(); }, 350);
      }, 3800);
    }
    window.ssToast = showToast;
    Array.prototype.forEach.call(document.querySelectorAll('[data-autotoast]'), function (el) {
      showToast(el.getAttribute('data-autotoast'), el.getAttribute('data-autotoast-type') === 'error');
      el.remove();
    });

    /* ---------- конфетти (canvas) ---------- */
    var cvs = document.getElementById('ss-confetti-canvas');
    var ctx = cvs ? cvs.getContext('2d') : null;
    var raf = null, flakes = [];
    function tick() {
      ctx.clearRect(0, 0, cvs.width, cvs.height);
      flakes = flakes.filter(function (p) { return p.life > 0 && p.y < cvs.height + 20; });
      for (var i = 0; i < flakes.length; i++) {
        var p = flakes[i];
        p.life--; p.vy += p.g; p.x += p.vx; p.y += p.vy; p.vx *= .99; p.r += p.vr;
        ctx.save();
        ctx.translate(p.x, p.y);
        ctx.rotate(p.r);
        ctx.fillStyle = p.c;
        ctx.globalAlpha = Math.min(1, p.life / 40);
        ctx.fillRect(-p.s / 2, -p.s / 2, p.s, p.s * .62);
        ctx.restore();
      }
      if (flakes.length) { raf = requestAnimationFrame(tick); }
      else { raf = null; ctx.clearRect(0, 0, cvs.width, cvs.height); }
    }
    function burst(x, y) {
      if (!ctx) return;
      cvs.width = window.innerWidth;
      cvs.height = window.innerHeight;
      var colors = ['#FFD93D', '#FFB347', '#FF8C42', '#223A6D', '#4A5D9E', '#FFFFFF'];
      for (var i = 0; i < 140; i++) {
        var a = Math.random() * Math.PI * 2;
        var sp = 4 + Math.random() * 7;
        flakes.push({
          x: x, y: y,
          vx: Math.cos(a) * sp, vy: Math.sin(a) * sp - 4,
          g: .16, s: 4 + Math.random() * 5,
          c: colors[i % colors.length],
          r: Math.random() * Math.PI, vr: (Math.random() - .5) * .3,
          life: 90 + Math.random() * 40
        });
      }
      if (!raf) raf = requestAnimationFrame(tick);
    }
    window.ssConfetti = function () {
      if (reduce || !ctx) return;
      burst(window.innerWidth / 2, window.innerHeight / 3);
    };
    if (document.querySelector('[data-confetti]') && !reduce) {
      setTimeout(function () { window.ssConfetti(); }, 350);
    }
  });
})();
