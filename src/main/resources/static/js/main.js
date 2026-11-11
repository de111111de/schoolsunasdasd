/* SCHOOLSUN — общие анимации интерфейса (тема, reveal, тосты, конфетти, параллакс) */
(function () {
  'use strict';

  var reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var finePointer = window.matchMedia && window.matchMedia('(pointer: fine)').matches;

  /* ---------- тема: свет «рассвета» / ночь со звёздами ---------- */
  try {
    if (localStorage.getItem('ss-theme') === 'night') {
      document.documentElement.setAttribute('data-night', '');
    }
  } catch (e) { /* приватный режим — просто остаёмся в светлой теме */ }

  document.addEventListener('DOMContentLoaded', function () {
    var themeBtn = document.getElementById('ss-theme');
    if (themeBtn) {
      themeBtn.addEventListener('click', function () {
        var root = document.documentElement;
        var night = root.toggleAttribute('data-night');
        themeBtn.setAttribute('aria-pressed', night ? 'true' : 'false');
        try { localStorage.setItem('ss-theme', night ? 'night' : 'day'); } catch (e) {}
      });
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

    /* ---------- параллакс в hero ---------- */
    var hero = document.getElementById('ss-hero');
    var par = document.getElementById('ss-hero-parallax');
    if (hero && par && !reduce && finePointer) {
      hero.addEventListener('mousemove', function (e) {
        var r = hero.getBoundingClientRect();
        var dx = (e.clientX - r.left) / r.width - .5;
        var dy = (e.clientY - r.top) / r.height - .5;
        par.style.transform = 'translate3d(' + (dx * 14).toFixed(1) + 'px,' + (dy * 9).toFixed(1) + 'px,0)';
      });
      hero.addEventListener('mouseleave', function () { par.style.transform = ''; });
    }

    /* ---------- 3D-наклон карточек ---------- */
    if (!reduce && finePointer) {
      Array.prototype.forEach.call(document.querySelectorAll('.ss-course'), function (card) {
        card.addEventListener('mousemove', function (e) {
          var r = card.getBoundingClientRect();
          var dx = (e.clientX - r.left) / r.width - .5;
          var dy = (e.clientY - r.top) / r.height - .5;
          card.style.transform = 'perspective(800px) rotateX(' + (-dy * 7).toFixed(2) + 'deg) rotateY('
            + (dx * 7).toFixed(2) + 'deg) translateY(-4px)';
        });
        card.addEventListener('mouseleave', function () { card.style.transform = ''; });
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

    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (!en.isIntersecting) return;
        var el = en.target;
        el.classList.add('in');
        Array.prototype.forEach.call(el.querySelectorAll('[data-target]'), function (c) {
          if (!c.getAttribute('data-done')) {
            c.setAttribute('data-done', '1');
            animateCounter(c);
          }
        });
        Array.prototype.forEach.call(el.querySelectorAll('.ss-progress i[data-val]'), function (bar) {
          bar.style.width = bar.getAttribute('data-val') + '%';
        });
        Array.prototype.forEach.call(el.querySelectorAll('.ss-ring__val'), function (ring) {
          var C = 364.4;
          var v = parseFloat(ring.getAttribute('data-val')) || 0;
          ring.style.strokeDashoffset = (C * (1 - v / 100)).toFixed(1);
        });
        io.unobserve(el);
      });
    }, { threshold: .15 });
    Array.prototype.forEach.call(document.querySelectorAll('.ss-reveal'), function (el) {
      var ring = el.querySelector ? el.querySelector('.ss-ring__val') : null;
      if (ring && !ring.getAttribute('data-val')) ring.setAttribute('data-val', '72');
      io.observe(el);
    });

    /* ---------- иконки Lucide ---------- */
    if (window.lucide) {
      lucide.createIcons({ attrs: { width: 18, height: 18 } });
    }

    /* ---------- тосты ---------- */
    var toasts = document.getElementById('ss-toasts');
    function showToast(text, isError) {
      if (!toasts || !text) return;
      var t = document.createElement('div');
      t.className = 'ss-toast' + (isError ? ' ss-toast--error' : '');
      t.innerHTML = '<i data-lucide="' + (isError ? 'alert-circle' : 'check-circle-2') + '" aria-hidden="true"></i><div>' + text + '</div>';
      toasts.appendChild(t);
      if (window.lucide) lucide.createIcons({ attrs: { width: 18, height: 18 } });
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
