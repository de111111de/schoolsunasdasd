/* SCHOOLSUN — конструктор тестов (панель учителя) */
(function () {
  'use strict';

  var TYPES = [
    { id: 'SINGLE', label: 'Один вариант' },
    { id: 'MULTI', label: 'Несколько вариантов' },
    { id: 'MATCH', label: 'Соответствие' },
    { id: 'NUMBER', label: 'Ввод числа' },
    { id: 'TEXT', label: 'Краткий ответ' },
    { id: 'OPEN', label: 'Открытый ответ (проверка учителем)' }
  ];

  function el(tag, cls, html) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (html !== undefined) e.innerHTML = html;
    return e;
  }

  function field(labelTxt, input) {
    var wrap = el('div', 'ss-field');
    var label = el('label', null, labelTxt);
    wrap.appendChild(label);
    wrap.appendChild(input);
    return wrap;
  }

  function makeQuestion(data) {
    data = data || { type: 'SINGLE', text: '', points: 2, answerText: '', options: null };
    var card = el('div', 'ss-qitem');
    card.className = 'ss-qitem';

    var head = el('div', 'ss-qitem__head');

    var typeSel = el('select', 'ss-select');
    typeSel.style.flex = '0 0 250px';
    TYPES.forEach(function (t) {
      var o = el('option', null, t.label);
      o.value = t.id;
      if (data.type === t.id) o.selected = true;
      typeSel.appendChild(o);
    });

    var textInp = el('input', 'ss-input');
    textInp.type = 'text';
    textInp.placeholder = 'Текст вопроса';
    textInp.value = data.text || '';

    var pointsInp = el('input', 'ss-input');
    pointsInp.type = 'number';
    pointsInp.min = '1';
    pointsInp.max = '100';
    pointsInp.value = data.points || 1;
    pointsInp.style.flex = '0 0 90px';
    pointsInp.title = 'Баллы';

    var del = el('button', 'ss-iconbtn', '✕');
    del.type = 'button';
    del.title = 'Удалить вопрос';
    del.addEventListener('click', function () { card.remove(); });

    head.appendChild(typeSel);
    head.appendChild(field('Вопрос', textInp));
    head.appendChild(field('Баллы', pointsInp));
    head.appendChild(del);
    card.appendChild(head);

    var body = el('div', 'ss-qitem__body');
    card.appendChild(body);

    var answerWrap = field('Правильный ответ (число или слово)', el('input', 'ss-input'));
    var answerInp = answerWrap.querySelector('input');
    answerInp.type = 'text';
    answerInp.value = data.answerText || '';
    body.appendChild(answerWrap);

    var optionsWrap = el('div');
    body.appendChild(optionsWrap);

    function addOption(od) {
      od = od || { text: '', correct: false, pairKey: '' };
      var row = el('div', 'ss-optrow');
      var isMatch = typeSel.value === 'MATCH';

      var flag = document.createElement('input');
      flag.type = isMatch ? 'hidden' : 'checkbox';
      flag.checked = !!od.correct;
      flag.title = 'Правильный вариант';
      if (isMatch) flag.type = 'hidden';

      var txt = el('input', 'ss-input');
      txt.type = 'text';
      txt.placeholder = isMatch ? 'Левая часть (например: команда)' : 'Текст варианта';
      txt.value = od.text || '';

      row.appendChild(flag);
      row.appendChild(txt);

      if (isMatch) {
        var pair = el('input', 'ss-input');
        pair.type = 'text';
        pair.placeholder = 'Правая часть (результат)';
        pair.className = 'ss-input ss-opt__pair';
        pair.value = od.pairKey || '';
        row.appendChild(pair);
      }

      var rm = el('button', 'ss-iconbtn', '✕');
      rm.type = 'button';
      rm.title = 'Удалить вариант';
      rm.addEventListener('click', function () { row.remove(); });
      row.appendChild(rm);
      optionsWrap.appendChild(row);
    }

    function addOptionBtn(label) {
      var b = el('button', 'ss-btn ss-btn--ghost ss-btn--sm', label);
      b.type = 'button';
      b.addEventListener('click', function () { addOption(); });
      return b;
    }

    var addSingle = addOptionBtn('+ вариант');
    var addMatch = addOptionBtn('+ пара');
    body.appendChild(addSingle);
    body.appendChild(addMatch);

    function sync() {
      var t = typeSel.value;
      var withOptions = t === 'SINGLE' || t === 'MULTI' || t === 'MATCH';
      var withAnswer = t === 'NUMBER' || t === 'TEXT';
      answerWrap.style.display = withAnswer ? '' : 'none';
      optionsWrap.style.display = withOptions ? '' : 'none';
      addSingle.style.display = t === 'MATCH' ? 'none' : '';
      addMatch.style.display = t === 'MATCH' ? '' : 'none';
      if (withOptions && optionsWrap.children.length === 0) {
        if (t === 'MATCH') { addOption(); addOption(); addOption(); }
        else { addOption(); addOption(); addOption(); }
      }
    }

    typeSel.addEventListener('change', function () {
      optionsWrap.innerHTML = '';
      sync();
    });
    sync();

    if (data.options) {
      data.options.forEach(addOption);
    }

    card.qData = function () {
      var opts = [];
      optionsWrap.querySelectorAll('.ss-optrow').forEach(function (row) {
        var inputs = row.querySelectorAll('input[type="text"]');
        var txt = inputs[0].value.trim();
        if (!txt) return;
        var o = { text: txt, correct: false, pairKey: '' };
        var cb = row.querySelector('input[type="checkbox"]');
        o.correct = cb ? cb.checked : false;
        if (typeSel.value === 'MATCH' && inputs[1]) {
          o.pairKey = inputs[1].value.trim();
        }
        opts.push(o);
      });
      return {
        type: typeSel.value,
        text: textInp.value.trim(),
        points: parseInt(pointsInp.value, 10) || 1,
        answerText: answerInp.value.trim(),
        options: opts
      };
    };

    return card;
  }

  document.addEventListener('DOMContentLoaded', function () {
    var root = document.getElementById('ss-quiz-builder');
    if (!root) return;

    var list = document.getElementById('qb-questions');
    var container = root;

    var preset = container.getAttribute('data-quiz');
    if (preset && preset !== '') {
      try {
        var data = JSON.parse(preset);
        var t = document.getElementById('qb-title');
        if (t) t.value = data.title || '';
        var time = document.getElementById('qb-time');
        if (time) time.value = data.timeLimitSec ? Math.round(data.timeLimitSec / 60) : 0;
        var at = document.getElementById('qb-attempts');
        if (at) at.value = data.maxAttempts || 3;
        var sh = document.getElementById('qb-show');
        if (sh) sh.checked = data.showAnswers !== false;
        (data.questions || []).forEach(function (q) {
          list.appendChild(makeQuestion(q));
        });
      } catch (e) { /* повреждённый предсет — начнём с пустого конструктора */ }
    }
    if (!list.children.length) {
      list.appendChild(makeQuestion());
    }

    document.getElementById('qb-add').addEventListener('click', function () {
      list.appendChild(makeQuestion());
      list.lastElementChild.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });

    document.getElementById('qb-save').addEventListener('click', function () {
      var status = document.getElementById('qb-status');
      var questions = [];
      var bad = false;
      list.querySelectorAll(':scope > .ss-qitem').forEach(function (card) {
        if (!card.qData) return;
        var d = card.qData();
        if (!d.text) { bad = true; return; }
        if ((d.type === 'NUMBER' || d.type === 'TEXT') && !d.answerText) { bad = true; return; }
        questions.push(d);
      });
      if (bad || !questions.length) {
        status.textContent = 'Заполни текст каждого вопроса (и правильный ответ для чисел/кратких ответов)';
        return;
      }
      var payload = {
        title: document.getElementById('qb-title').value.trim() || 'Тест',
        timeLimitSec: (parseInt(document.getElementById('qb-time').value, 10) || 0) * 60,
        maxAttempts: parseInt(document.getElementById('qb-attempts').value, 10) || 3,
        showAnswers: document.getElementById('qb-show').checked,
        questions: questions
      };
      status.textContent = 'Сохраняю…';
      var token = (document.querySelector('meta[name="_csrf"]') || {}).content || '';
      var headerName = (document.querySelector('meta[name="_csrf_header"]') || {}).content || 'X-CSRF-TOKEN';
      var headers = { 'Content-Type': 'application/json' };
      if (token) headers[headerName] = token;
      fetch(container.getAttribute('data-save-url'), {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(payload)
      }).then(function (r) {
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.json();
      }).then(function (res) {
        status.textContent = '';
        if (window.ssToast) window.ssToast('Тест сохранён!');
        setTimeout(function () { window.location = res.redirect; }, 600);
      }).catch(function () {
        status.textContent = 'Не удалось сохранить — проверь подключение и попробуй ещё раз';
      });
    });
  });
})();
