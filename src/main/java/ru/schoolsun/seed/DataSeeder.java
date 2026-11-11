package ru.schoolsun.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.schoolsun.course.Course;
import ru.schoolsun.course.CourseRepository;
import ru.schoolsun.course.Enrollment;
import ru.schoolsun.course.EnrollmentRepository;
import ru.schoolsun.course.Subject;
import ru.schoolsun.homework.Homework;
import ru.schoolsun.homework.HomeworkForm;
import ru.schoolsun.homework.HomeworkRepository;
import ru.schoolsun.homework.HomeworkService;
import ru.schoolsun.homework.HomeworkSubmission;
import ru.schoolsun.homework.HomeworkSubmissionRepository;
import ru.schoolsun.homework.SubmissionStatus;
import ru.schoolsun.lesson.Lesson;
import ru.schoolsun.lesson.LessonRepository;
import ru.schoolsun.lesson.LessonService;
import ru.schoolsun.quiz.AttemptStatus;
import ru.schoolsun.quiz.QuizAttempt;
import ru.schoolsun.quiz.QuizAttemptRepository;
import ru.schoolsun.quiz.QuizService;
import ru.schoolsun.user.Role;
import ru.schoolsun.user.User;
import ru.schoolsun.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Демо-данные SCHOOLSUN: наполняются при первом запуске (когда в БД нет пользователей).
 * Пароль всех демо-аккаунтов: test1234
 *
 * 15 курсов по 5 предметам (математика, физика, информатика, химия, биология),
 * 7–11 класс, создатели — учителя Леонид Королев и Егор Веремчук.
 * Три спецкурса видны только по ссылке-приглашению /join/{code}.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private final UserRepository users;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final LessonRepository lessons;
    private final HomeworkRepository homeworks;
    private final QuizAttemptRepository attempts;
    private final HomeworkSubmissionRepository submissions;
    private final QuizService quizService;
    private final HomeworkService homeworkService;
    private final LessonService lessonService;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, CourseRepository courses, EnrollmentRepository enrollments,
                      LessonRepository lessons, HomeworkRepository homeworks, QuizAttemptRepository attempts,
                      HomeworkSubmissionRepository submissions, QuizService quizService,
                      HomeworkService homeworkService, LessonService lessonService, PasswordEncoder encoder) {
        this.users = users;
        this.courses = courses;
        this.enrollments = enrollments;
        this.lessons = lessons;
        this.homeworks = homeworks;
        this.attempts = attempts;
        this.submissions = submissions;
        this.quizService = quizService;
        this.homeworkService = homeworkService;
        this.lessonService = lessonService;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }

        User admin = user("admin@schoolsun.ru", "Админ SCHOOLSUN", Role.ADMIN);
        User leonid = user("leonid@schoolsun.ru", "Леонид Королев", Role.TEACHER);
        User egor = user("egor@schoolsun.ru", "Егор Веремчук", Role.TEACHER);
        User anna = user("anna@schoolsun.ru", "Аня Солнцева", Role.STUDENT);
        User timur = user("timur@schoolsun.ru", "Тимур Лучников", Role.STUDENT);
        User liza = user("liza@schoolsun.ru", "Лиза Зорина", Role.STUDENT);
        User max = user("max@schoolsun.ru", "Макс Ветров", Role.STUDENT);
        User oleg = user("oleg@schoolsun.ru", "Олег Лунин", Role.STUDENT);

        // ================== МАТЕМАТИКА ==================
        Course c1 = course("algebra-ot-funkciy-k-olimpiadam", leonid,
                "Алгебра: от функций к олимпиадам", 8, "sun", false, null,
                "Разбираемся с функциями, графиками и нестандартными задачами — от базовых понятий до олимпиадных приёмов.");
        Course c2 = course("geometriya-dokazatelstva-bez-straha", egor,
                "Геометрия: доказательства без страха", 7, "sun", false, null,
                "Учимся строго доказывать: треугольники, углы, подобие. Доказательство — это логическая игра, и она затягивает.");
        Course c3 = course("oge-matematika-razbor-zadaniy", leonid,
                "ОГЭ: математика, разбор заданий", 9, "ember", false, null,
                "Разбираем все типы заданий ОГЭ по математике: ловушки, лайфхаки и полные решения.");

        // ================== ФИЗИКА ==================
        Course c4 = course("mehanika-pochemu-vse-dvizhetsya", egor,
                "Механика: почему всё движется", 7, "sky", true, null,
                "Скорость, ускорение и законы Ньютона — на примерах, опытах и задачах из жизни.");
        Course c5 = course("elektrichestvo-ot-lampochki-do-cepey", leonid,
                "Электричество: от лампочки до цепей", 8, "sky", false, null,
                "Ток, напряжение, сопротивление: собираем цепи на бумаге и понимаем, как работает всё вокруг.");
        Course c6 = course("olimpiadnaya-fizika-speckurs", egor,
                "Олимпиадная физика (спецкурс)", 10, "ember", false, "phys-olymp-26",
                "Закрытый спецкурс для олимпиадной сборной: кинематика посложнее, энергия, нестандартные задачи.");

        // ================== ИНФОРМАТИКА ==================
        Course c7 = course("python-s-nulya-do-pervogo-proekta", leonid,
                "Python с нуля до первого проекта", 9, "ember", false, null,
                "Пишем первые программы: переменные, условия, циклы — и в конце соберём собственную игру.");
        Course c8 = course("sayty-na-html-i-css", egor,
                "Сайты на HTML и CSS", 7, "sun", false, null,
                "Своя первая страница в интернете: разметка, стили, анимации — всё своими руками.");
        Course c9 = course("algoritmy-i-struktury-dannyh-speckurs", leonid,
                "Алгоритмы и структуры данных (спецкурс)", 11, "sky", false, "algo-pro-26",
                "Закрытый спецкурс для старших: сортировки, графы, динамическое программирование — уровень олимпиад и собеседований.");

        // ================== ХИМИЯ ==================
        Course c10 = course("himiya-pervye-reakcii", egor,
                "Химия: первые реакции", 8, "ember", false, null,
                "Атомы, молекулы и первые реакции: почему ржавеет гвоздь и шипит сода с уксусом.");
        Course c11 = course("organicheskaya-himiya-dlya-novochkov", leonid,
                "Органическая химия для новичков", 10, "sun", false, null,
                "Углеводороды, спирты, кислоты — химия жизни без зубрёжки.");
        Course c12 = course("zanimatelnye-opyty-po-himii", egor,
                "Занимательные опыты по химии", 7, "sky", false, null,
                "Безопасные опыты, которые можно показать даже дома: фараоновы змейки, невидимые чернила и другое.");

        // ================== БИОЛОГИЯ ==================
        Course c13 = course("biologiya-kletka-i-organizm", egor,
                "Биология: клетка и организм", 7, "sun", false, null,
                "Путешествие внутрь клетки: органоиды, деление, ткани — фундамент всей биологии.");
        Course c14 = course("anatomiya-cheloveka-kak-my-ustroeny", leonid,
                "Анатомия человека: как мы устроены", 8, "ember", false, null,
                "Сердце-насос, мозг-компьютер и 600 мышц: изучаем себя изнутри.");
        Course c15 = course("genetika-tayny-nasledstvennosti-speckurs", egor,
                "Генетика: тайны наследственности (спецкурс)", 11, "sky", false, "gen-pro-26",
                "Закрытый спецкурс: законы Менделя, ДНК, разбор генетических задач уровня олимпиад.");

        // ---------- уроки ----------

        // c1 алгебра (Леонид)
        Lesson c1l1 = lesson(c1, "Что такое функция",
                "## Привет!\n\n**Функция** — это правило, которое каждому числу ставит в соответствие ровно одно другое число.\n\n- `f(x) = 2x + 3` — функция\n- Подставь `x = 5` и получишь `13`\n\n> Функция как автомат: бросил монетку — получил кофе.");
        Lesson c1l2 = lesson(c1, "Линейная функция и её график",
                "## Прямая линия\n\nЛинейная функция: `y = kx + b`\n\n1. `k` — наклон прямой\n2. `b` — где прямая пересекает ось Y\n\nПострой график `y = 2x` и проверь себя!");
        Lesson c1l3 = lesson(c1, "Квадратичная функция",
                "## Парабола\n\n`y = ax² + bx + c` — график называется **парабола**.\n\nЕсли `a > 0` — ветви вверх (улыбка), если `a < 0` — вниз (грусть).");
        lesson(c1, "Задачи на движение",
                "## Скорость, время, расстояние\n\n`S = v · t`\n\nКлассическая схема: два объекта едут навстречу — скорости складываются.");
        quiz(c1l2, "Проверка: линейная функция", 300, 3, true, List.of(
                new QuizService.Q("SINGLE", "Чему равен угловой коэффициент функции y = -3x + 5?", 2, null, List.of(
                        new QuizService.O("-3", true, null),
                        new QuizService.O("5", false, null),
                        new QuizService.O("3", false, null))),
                new QuizService.Q("MULTI", "Какие точки лежат на графике y = 2x?", 3, null, List.of(
                        new QuizService.O("(0; 0)", true, null),
                        new QuizService.O("(1; 2)", true, null),
                        new QuizService.O("(2; 1)", false, null))),
                new QuizService.Q("NUMBER", "Найди y, если x = 4 и y = 2x + 1", 2, "9", null)));
        homework(c1l3, "Домашка: построить графики", 10,
                "Построй в тетради графики функций y = x, y = 2x + 1, y = -x. Сфотографируй и отправь.");

        // c2 геометрия (Егор)
        Lesson c2l1 = lesson(c2, "Треугольники и их виды",
                "## Треугольник\n\nТри стороны, три угла — а сколько всего можно доказать!\n\nСумма углов любого треугольника равна **180°**. Запомни — пригодится в каждом доказательстве.");
        lesson(c2, "Признаки равенства треугольников",
                "## Три признака\n\n1. По двум сторонам и углу между ними\n2. По стороне и двум прилежащим углам\n3. По трём сторонам\n\n> Доказательство — это цепочка «смотри → замечай → применяй».");
        lesson(c2, "Подобие и пропорции",
                "## Похожие фигуры\n\nПодобные треугольники: углы равны, стороны пропорциональны.\n\nКоэффициент подобия `k` работает и для сторон, и для высот, и для медиан.");
        quiz(c2l1, "Треугольники: разминка", null, 3, true, List.of(
                new QuizService.Q("SINGLE", "Чему равна сумма углов треугольника?", 2, null, List.of(
                        new QuizService.O("180°", true, null),
                        new QuizService.O("360°", false, null),
                        new QuizService.O("90°", false, null))),
                new QuizService.Q("NUMBER", "Один угол треугольника 60°, второй 70°. Найди третий (в градусах)", 2, "50", null)));

        // c3 ОГЭ (Леонид)
        Lesson c3l1 = lesson(c3, "Как устроен ОГЭ",
                "## План победы\n\nПервая часть — 19 заданий на 1 балл. Вторая часть — 6 заданий на 2 балла.\n\nСтратегия: сначала проходим первую часть без ошибок, потом охотимся за баллами второй.");
        Lesson c3l2 = lesson(c3, "Числа и вычисления (задания 6–8)",
                "## Считаем быстро\n\nДроби, проценты, степени — задания 6–8. Здесь теряют баллы чаще всего из-за спешки.\n\nСовет: перепроверяй знак и порядок действий.");
        lesson(c3, "Геометрия в ОГЭ",
                "## Геометрические задания\n\nЗадания 15–18: площади, углы, вектора на клетчатой бумаге.\n\nСекрет: рисунок по клеточкам — почти всегда устная задача.");
        homework(c3l2, "Тренировка: задания 6–8", 10,
                "Реши 10 вариантов задания 6 из сборника. Запиши ответы текстом, решение — фото.");

        // c4 механика (Егор)
        Lesson c4l1 = lesson(c4, "Скорость и путь",
                "## Движение\n\n**Скорость** = путь / время.\n\nМашина едет 60 км/ч — значит, за час проезжает 60 км.\n\nФормула: `v = S / t`");
        Lesson c4l2 = lesson(c4, "Ускорение",
                "## Быстрее!\n\n**Ускорение** — как быстро меняется скорость.\n\n`a = (v − v₀) / t`\n\nСамолёт на взлёте: скорость растёт каждую секунду — это и есть ускорение.");
        lesson(c4, "Первый закон Ньютона",
                "## Инерция\n\nТело остаётся в покое или движется равномерно, пока на него не подействует сила.\n\n> Автобус резко тормозит — ты продолжаешь ехать вперёд. Это инерция!");
        quiz(c4l1, "Скорость и путь", null, 2, true, List.of(
                new QuizService.Q("SINGLE", "Велосипедист проехал 30 км за 2 часа. Его скорость?", 2, null, List.of(
                        new QuizService.O("15 км/ч", true, null),
                        new QuizService.O("60 км/ч", false, null),
                        new QuizService.O("32 км/ч", false, null))),
                new QuizService.Q("TEXT", "Как называется прибор для измерения скорости? (одно слово)", 2,
                        "спидометр", null)));
        homework(c4l2, "Задачник: ускорение", 10,
                "Реши три задачи из учебника §5 (№3, №5, №7). Ответы запиши текстом или отправь фото решения.");

        // c5 электричество (Леонид)
        Lesson c5l1 = lesson(c5, "Электрический ток",
                "## Ток\n\n**Ток** — направленное движение зарядов. Измеряется в амперах (А).\n\nАналогия: трубы с водой. Ток — сколько воды течёт, напряжение — под каким давлением.");
        Lesson c5l2 = lesson(c5, "Закон Ома",
                "## I = U / R\n\n**Закон Ома**: сила тока равна напряжению, делённому на сопротивление.\n\n- U — напряжение (вольты)\n- R — сопротивление (омы)\n- I — ток (амперы)");
        lesson(c5, "Последовательное и параллельное соединение",
                "## Два способа соединить\n\nПоследовательно: R = R₁ + R₂ (сопротивления складываются).\n\nПараллельно: 1/R = 1/R₁ + 1/R₂ (общее сопротивление меньше каждого).");
        quiz(c5l2, "Закон Ома", 240, 3, true, List.of(
                new QuizService.Q("SINGLE", "Напряжение 12 В, сопротивление 4 Ом. Сила тока?", 2, null, List.of(
                        new QuizService.O("3 А", true, null),
                        new QuizService.O("48 А", false, null),
                        new QuizService.O("0.33 А", false, null))),
                new QuizService.Q("NUMBER", "U = 10 В, I = 2 А. Найди R в омах", 2, "5", null)));

        // c6 олимпиадная физика (Егор, спецкурс)
        Lesson c6l1 = lesson(c6, "Кинематика: графики движения",
                "## Читаем графики\n\nГрафик скорости — самый честный рассказ о движении.\n\nПлощадь под графиком v(t) — это пройденный путь. Проверь на треугольнике разгона!");
        lesson(c6, "Законы сохранения в задачах",
                "## Энергия и импульс\n\n`E = mv²/2` — кинетическая энергия, `p = mv` — импульс.\n\nВ закрытой системе импульс сохраняется всегда — на этом держится половина олимпиадных задач.");

        // c7 python (Леонид)
        Lesson c7l1 = lesson(c7, "Первая программа",
                "## print()\n\nТвоя первая программа:\n\n```python\nprint(\"Привет, SCHOOLSUN!\")\n```\n\n`print` выводит текст на экран. Запусти и попробуй вывести своё имя!");
        Lesson c7l2 = lesson(c7, "Переменные и типы",
                "## Ящики для данных\n\n```python\nname = \"Аня\"\nage = 14\nheight = 1.62\n```\n\n- `str` — текст\n- `int` — целые числа\n- `float` — дробные");
        Lesson c7l3 = lesson(c7, "Условия if / else",
                "## Если... то...\n\n```python\nif age >= 18:\n    print(\"Взрослый\")\nelse:\n    print(\"Школьник\")\n```\n\nНе забывай про отступы — в Python они часть языка!");
        Lesson c7l4 = lesson(c7, "Циклы for и while",
                "## Повторяем без скуки\n\n```python\nfor i in range(5):\n    print(\"Солнце\", i)\n```\n\nЦикл выполнит блок кода 5 раз: от 0 до 4.");
        lesson(c7, "Проект: игра «Угадай число»",
                "## Финальный проект\n\nСобери игру:\n\n```python\nimport random\nsecret = random.randint(1, 100)\n# твой код здесь\n```\n\nПрограмма загадывает число, игрок угадывает. Удачи!");
        quiz(c7l1, "Знакомство с Python", 240, 3, true, List.of(
                new QuizService.Q("SINGLE", "Какая команда выводит текст на экран?", 2, null, List.of(
                        new QuizService.O("print()", true, null),
                        new QuizService.O("say()", false, null),
                        new QuizService.O("show()", false, null))),
                new QuizService.Q("MATCH", "Соедини команду и результат", 3, null, List.of(
                        new QuizService.O("print(\"2\")", false, "печатает 2"),
                        new QuizService.O("2 + 3", false, "даёт 5"),
                        new QuizService.O("# привет", false, "это комментарий")))));
        homework(c7l4, "Практика: циклы", 10,
                "Напиши программу, которая выводит таблицу умножения на 7. Код вставь текстом или загрузи файл .py");

        // c8 html/css (Егор)
        Lesson c8l1 = lesson(c8, "Что такое HTML",
                "## Разметка\n\n```html\n<h1>Заголовок</h1>\n<p>Абзац текста</p>\n```\n\nHTML — это скелет страницы: заголовки, абзацы, картинки, ссылки.");
        lesson(c8, "Стили CSS: цвет и шрифты",
                "## CSS\n\n```css\nh1 {\n  color: #FF8C42;\n  font-family: Arial;\n}\n```\n\nCSS — это одежда страницы: цвета, размеры, отступы.");
        lesson(c8, " Flexbox: раскладка блоков",
                "## Раскладка\n\n```css\n.container {\n  display: flex;\n  gap: 16px;\n}\n```\n\nFlexbox расставляет элементы в ряд или колонку — главный инструмент вёрстки.");
        quiz(c8l1, "Основы HTML", null, 3, true, List.of(
                new QuizService.Q("SINGLE", "Какой тег делает заголовок первого уровня?", 2, null, List.of(
                        new QuizService.O("<h1>", true, null),
                        new QuizService.O("<header>", false, null),
                        new QuizService.O("<title1>", false, null))),
                new QuizService.Q("TEXT", "Какой язык отвечает за стили страницы? (аббревиатура)", 2, "css", null)));

        // c9 алгоритмы (Леонид, спецкурс)
        Lesson c9l1 = lesson(c9, "Оценка сложности: O-большое",
                "## Считаем шаги\n\n`O(n)` — линейно, `O(n²)` — квадратично, `O(log n)` — логарифмически.\n\n> При n = 1 000 000: разница между O(n) и O(n²) — секунды против недель.");
        lesson(c9, "Бинарный поиск и сортировки",
                "## Дели и властвуй\n\nБинарный поиск ищет в отсортированном массиве за `O(log n)`.\n\nСортировка слиянием: делим пополам, сливаем — стабильные `O(n log n)`.");

        // c10 химия (Егор)
        Lesson c10l1 = lesson(c10, "Атомы и молекулы",
                "## Из всего состоит\n\nАтом — ядро (протоны, нейтроны) и электроны вокруг.\n\nМолекула воды H₂O: два водорода и один кислород — и это тема всей главы.");
        lesson(c10, "Химические формулы",
                "## Читаем формулы\n\n- H₂SO₄ — серная кислота\n- NaCl — поваренная соль\n\nИндексы показывают, сколько атомов каждого элемента в молекуле.");
        Lesson c10l3 = lesson(c10, "Первые реакции",
                "## Реакция!\n\nПризнаки реакции: газ, осадок, изменение цвета, тепло.\n\nСода + уксус → пузырьки CO₂. Это уже химия!");
        quiz(c10l3, "Признаки реакций", null, 2, true, List.of(
                new QuizService.Q("MULTI", "Что может быть признаком химической реакции?", 3, null, List.of(
                        new QuizService.O("Выделение газа", true, null),
                        new QuizService.O("Изменение цвета", true, null),
                        new QuizService.O("Растворение сахара в воде", false, null))),
                new QuizService.Q("SINGLE", "Что образуется при реакции соды с уксусом?", 2, null, List.of(
                        new QuizService.O("Углекислый газ", true, null),
                        new QuizService.O("Кислород", false, null),
                        new QuizService.O("Водород", false, null)))));

        // c11 органика (Леонид)
        Lesson c11l1 = lesson(c11, "Углеводороды",
                "## C и H\n\nМетан CH₄ — самый простой углеводород, основа природного газа.\n\nГомологи отличаются на группу CH₂: метан, этан, пропан, бутан...");
        lesson(c11, "Спирты и кислоты",
                "## Функциональные группы\n\nСпирты несут группу -OH (этанол), кислоты — -COOH (уксусная кислота).\n\nИменно группа определяет характер вещества.");

        // c12 опыты (Егор)
        Lesson c12l1 = lesson(c12, "Невидимые чернила",
                "## Секретное письмо\n\nПиши лимонным соком на бумаге, нагрей — текст проявится коричневым.\n\nПочему? Органика лимона окисляется при нагреве быстрее бумаги.");
        lesson(c12, "Фараонова змейка",
                "## Растущая змея\n\nСухое горючее (гексаметилентетрамин) + порошок — при поджиге вырастает серая «змейка».\n\nЭто термическое разложение с образованием угля. Только с учителем!");

        // c13 биология (Егор)
        Lesson c13l1 = lesson(c13, "Клетка: город из органоидов",
                "## Внутри клетки\n\n- Митохондрии — электростанции\n- Ядро — ратуша с ДНК\n- Рибосомы — фабрики белка\n\nКлетка — целая вселенная под микроскопом.");
        Lesson c13l2 = lesson(c13, "Деление клетки",
                "## Митоз\n\nОдна клетка → две одинаковые. Так растут все ткани организма.\n\nПеред делением ДНК удваивается — каждая новая клетка получает полную копию.");
        lesson(c13, "Ткани и органы",
                "## От клетки к организму\n\nКлетки → ткани → органы → системы органов → организм.\n\nЧетыре основных типа тканей: эпителий, соединительная, мышечная, нервная.");
        quiz(c13l1, "Органоиды клетки", null, 2, true, List.of(
                new QuizService.Q("SINGLE", "Какой органоид называют «электростанцией клетки»?", 2, null, List.of(
                        new QuizService.O("Митохондрия", true, null),
                        new QuizService.O("Рибосома", false, null),
                        new QuizService.O("Ядро", false, null))),
                new QuizService.Q("TEXT", "Где хранится наследственная информация клетки? (одно слово)", 2, "ядро", null)));

        // c14 анатомия (Леонид)
        Lesson c14l1 = lesson(c14, "Сердце и кровообращение",
                "## Насос размером с кулак\n\nСердце делает ~100 000 ударов в день и гоняет 7 000 литров крови.\n\nДва круга кровообращения: большой (по телу) и малый (через лёгкие).");
        lesson(c14, "Мозг и нервная система",
                "## Компьютер из 86 миллиардов нейронов\n\nНервный импульс летит до 120 м/с.\n\nМозг потребляет 20% всей энергии тела — думай, но не забывай есть.");

        // c15 генетика (Егор, спецкурс)
        Lesson c15l1 = lesson(c15, "Законы Менделя",
                "## От гороха к человеку\n\nПервый закон: единообразие гибридов F₁.\n\nВторой закон: расщепление 3:1 в F₂. Решай задачи на решётку Пеннета!");
        lesson(c15, "ДНК и код жизни",
                "## Двойная спираль\n\nДНК — инструкция из 4 букв: A, T, G, C.\n\nТри буквы = кодон = одна аминокислота. Так из кода собираются белки.");

        // ---------- записи на курсы ----------
        enroll(anna, c1); enroll(anna, c4); enroll(anna, c5); enroll(anna, c10);
        enroll(timur, c1); enroll(timur, c7); enroll(timur, c8);
        enroll(liza, c1); enroll(liza, c4); enroll(liza, c7); enroll(liza, c13);
        enroll(max, c2); enroll(max, c7); enroll(max, c8); enroll(max, c12);
        enroll(oleg, c2); enroll(oleg, c4); enroll(oleg, c10); enroll(oleg, c13); enroll(oleg, c3);
        // спецкурсы — ученики, которые «пришли по ссылке»
        enroll(anna, c6); enroll(liza, c6);
        enroll(timur, c9); enroll(max, c9);
        enroll(liza, c15);

        // ---------- попытки тестов ----------
        attempt(c1l2, anna, 1, 6);
        attempt(c1l2, timur, 1, 5);
        attempt(c1l2, liza, 1, 7);
        attempt(c2l1, max, 1, 4);
        attempt(c2l1, oleg, 1, 4);
        attempt(c4l1, anna, 1, 4);
        attempt(c4l1, oleg, 1, 2);
        attempt(c5l2, anna, 1, 3);
        attempt(c7l1, timur, 1, 5);
        attempt(c7l1, liza, 1, 5);
        attempt(c7l1, max, 1, 4);
        attempt(c8l1, timur, 1, 4);
        attempt(c8l1, max, 1, 4);
        attempt(c10l3, anna, 1, 5);
        attempt(c10l3, oleg, 1, 2);
        attempt(c13l1, liza, 1, 4);
        attempt(c13l1, oleg, 1, 2);

        // ---------- домашние работы ----------
        homeworks.findByLessonId(c1l3.getId()).ifPresent(hw -> {
            seedSubmission(hw, anna, 9, SubmissionStatus.ACCEPTED, "Отличное оформление графиков!");
            seedSubmission(hw, timur, 6, SubmissionStatus.REVISION, "Проверь масштаб на второй оси");
        });
        homeworks.findByLessonId(c4l2.getId()).ifPresent(hw ->
                seedSubmission(hw, liza, 10, SubmissionStatus.ACCEPTED, "Все задачи решены верно"));
        homeworks.findByLessonId(c7l4.getId()).ifPresent(hw -> {
            seedSubmission(hw, timur, 8, SubmissionStatus.ACCEPTED, "Хорошо, добавь пояснения к циклу while");
            seedSubmission(hw, max, 7, SubmissionStatus.SUBMITTED, null);
        });
        homeworks.findByLessonId(c3l2.getId()).ifPresent(hw ->
                seedSubmission(hw, oleg, 8, SubmissionStatus.ACCEPTED, "Почти идеально, смотри округление"));

        // ---------- прогресс через реальные правила платформы ----------
        lessonService.completeManually(c1l1, anna);
        lessonService.completeManually(c1l1, timur);
        lessonService.completeManually(c1l1, liza);
        lessonService.completeIfEligible(c1l2, anna);
        lessonService.completeIfEligible(c1l2, timur);
        lessonService.completeIfEligible(c1l2, liza);
        lessonService.completeIfEligible(c1l3, anna);
        lessonService.completeManually(c2l1, max);
        lessonService.completeIfEligible(c2l1, oleg);
        lessonService.completeIfEligible(c4l1, anna);
        lessonService.completeIfEligible(c4l2, liza);
        lessonService.completeManually(c5l1, anna);
        lessonService.completeIfEligible(c5l2, anna);
        lessonService.completeManually(c7l1, timur);
        lessonService.completeIfEligible(c7l1, timur);
        lessonService.completeIfEligible(c7l1, liza);
        lessonService.completeIfEligible(c7l4, timur);
        lessonService.completeManually(c8l1, max);
        lessonService.completeIfEligible(c8l1, timur);
        lessonService.completeIfEligible(c10l3, anna);
        lessonService.completeIfEligible(c13l1, liza);
    }

    private User user(String email, String name, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(name);
        u.setPasswordHash(encoder.encode("test1234"));
        u.setRole(role);
        u.setBlocked(false);
        u.setCreatedAt(LocalDateTime.now());
        return users.save(u);
    }

    private Course course(String slug, User teacher, String title, int grade, String accent,
                          boolean sequential, String inviteCode, String description) {
        Course c = new Course();
        c.setSlug(slug);
        c.setTeacher(teacher);
        c.setTitle(title);
        c.setDescription(description);
        c.setSubject(subjectOf(title, slug));
        c.setGrade(grade);
        c.setAccent(accent);
        c.setSequentialMode(sequential);
        c.setPublished(true);
        c.setInviteOnly(inviteCode != null);
        c.setInviteCode(inviteCode);
        c.setCreatedAt(LocalDateTime.now());
        return courses.save(c);
    }

    /** Предмет определяем по слагу курса (порядок создания в сидере фиксирован). */
    private Subject subjectOf(String title, String slug) {
        if (slug.contains("algebra") || slug.contains("geometriya") || slug.contains("oge-matematika")) {
            return Subject.MATH;
        }
        if (slug.contains("mehanika") || slug.contains("elektrichestvo") || slug.contains("olimpiadnaya-fizika")) {
            return Subject.PHYSICS;
        }
        if (slug.contains("python") || slug.contains("sayty") || slug.contains("algoritmy")) {
            return Subject.CS;
        }
        if (slug.contains("himiya") || slug.contains("organicheskaya") || slug.contains("opyty")) {
            return Subject.CHEMISTRY;
        }
        return Subject.BIOLOGY;
    }

    private Lesson lesson(Course course, String title, String theoryMd) {
        Lesson l = new Lesson();
        l.setCourse(course);
        l.setTitle(title);
        l.setPosition(lessons.findByCourseIdOrderByPositionAsc(course.getId()).size() + 1);
        l.setTheoryMd(theoryMd);
        l.setPublished(true);
        l.setCreatedAt(LocalDateTime.now());
        return lessons.save(l);
    }

    private void quiz(Lesson lesson, String title, Integer timeLimit, int maxAttempts, boolean showAnswers,
                      List<QuizService.Q> qs) {
        quizService.saveFromPayload(lesson,
                new QuizService.QuizPayload(title, timeLimit, maxAttempts, showAnswers, qs));
    }

    private void homework(Lesson lesson, String title, int maxPoints, String instruction) {
        HomeworkForm form = new HomeworkForm();
        form.setTitle(title);
        form.setInstruction(instruction);
        form.setMaxPoints(maxPoints);
        form.setDeadline(LocalDateTime.now().plusDays(7));
        homeworkService.save(lesson, form);
    }

    private void enroll(User student, Course course) {
        Enrollment e = new Enrollment();
        e.setCourse(course);
        e.setStudent(student);
        e.setCreatedAt(LocalDateTime.now());
        enrollments.save(e);
    }

    private void attempt(Lesson lesson, User student, int attemptNo, int score) {
        ru.schoolsun.quiz.Quiz quiz = lessonService.quizOf(lesson.getId());
        if (quiz == null) {
            return;
        }
        QuizAttempt a = new QuizAttempt();
        a.setQuiz(quiz);
        a.setStudent(student);
        a.setAttemptNo(attemptNo);
        a.setStartedAt(LocalDateTime.now().minusMinutes(10));
        a.setFinishedAt(LocalDateTime.now().minusMinutes(8));
        a.setScore(score);
        a.setStatus(AttemptStatus.FINISHED);
        attempts.save(a);
    }

    private void seedSubmission(Homework hw, User student, int score, SubmissionStatus status, String feedback) {
        HomeworkSubmission s = new HomeworkSubmission();
        s.setHomework(hw);
        s.setStudent(student);
        s.setTextAnswer("Демо-сдача: " + hw.getTitle());
        s.setSubmittedAt(LocalDateTime.now().minusDays(1));
        s.setStatus(status);
        s.setScore(score);
        s.setFeedback(feedback);
        s.setGradedAt(status != SubmissionStatus.SUBMITTED ? LocalDateTime.now().minusHours(5) : null);
        submissions.save(s);
    }
}
