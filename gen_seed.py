import struct, zlib, base64, json

def make_png(w, h, r, g, b):
    def chunk(name, data):
        c = zlib.crc32(name + data) & 0xffffffff
        return struct.pack('>I', len(data)) + name + data + struct.pack('>I', c)
    ihdr = struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0)
    raw = b''.join(b'\x00' + bytes([r, g, b] * w) for _ in range(h))
    idat = chunk(b'IDAT', zlib.compress(raw))
    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + idat + chunk(b'IEND', b'')
    return base64.b64encode(png).decode()

P = {
    'Скот':    make_png(160,120,139, 90, 43),
    'Зерно':   make_png(160,120,210,180, 50),
    'Овощи':   make_png(160,120, 60,140, 60),
    'Фрукты':  make_png(160,120,220, 80, 80),
    'Молоко':  make_png(160,120,220,230,255),
    'Птица':   make_png(160,120,255,165,  0),
    'Корма':   make_png(160,120,100,160, 80),
    'Техника': make_png(160,120, 80,100,120),
    'Услуги':  make_png(160,120,100,100,200),
}

data = {
  'users': {
    'admin001uid001': {'uid':'admin001uid001','name':'Администратор','firstName':'Администратор','phone':'+996700000001','email':'admin@farme.kg','region':'Бишкек','role':'admin','rating':5.0,'reviewCount':0,'listingCount':0,'avatar':None,'banned':False,'verified':True,'createdAt':1743465600000,'chatIds':{}},
    'user001uid001': {'uid':'user001uid001','name':'Айбек Карыбеков','firstName':'Айбек','phone':'+996701234567','email':'aibek@example.com','region':'Чуйская область','role':'user','rating':4.5,'reviewCount':2,'listingCount':8,'avatar':None,'banned':False,'verified':True,'createdAt':1743465600000,'chatIds':{'-Nchat001chatId00':True,'-Nchat002chatId00':True}},
    'user002uid002': {'uid':'user002uid002','name':'Гүлнара Исакова','firstName':'Гүлнара','phone':'+996702345678','email':'gulnara@example.com','region':'Иссык-Кульская область','role':'user','rating':4.8,'reviewCount':3,'listingCount':7,'avatar':None,'banned':False,'verified':True,'createdAt':1743724800000,'chatIds':{'-Nchat001chatId00':True,'-Nchat003chatId00':True}},
    'user003uid003': {'uid':'user003uid003','name':'Мирбек Токтосунов','firstName':'Мирбек','phone':'+996703456789','email':'mirbek@example.com','region':'Ошская область','role':'user','rating':4.2,'reviewCount':2,'listingCount':6,'avatar':None,'banned':False,'verified':True,'createdAt':1743984000000,'chatIds':{'-Nchat002chatId00':True}},
    'user004uid004': {'uid':'user004uid004','name':'Нурзат Асанова','firstName':'Нурзат','phone':'+996704567890','email':'nurzat@example.com','region':'Нарынская область','role':'user','rating':4.6,'reviewCount':1,'listingCount':5,'avatar':None,'banned':False,'verified':True,'createdAt':1744243200000,'chatIds':{'-Nchat003chatId00':True}},
    'user005uid005': {'uid':'user005uid005','name':'Бекзат Жолдошев','firstName':'Бекзат','phone':'+996705678901','email':'bekzat@example.com','region':'Джалал-Абадская область','role':'user','rating':4.3,'reviewCount':1,'listingCount':5,'avatar':None,'banned':False,'verified':False,'createdAt':1744502400000,'chatIds':{}},
    'user006uid006': {'uid':'user006uid006','name':'Зарина Мамытова','firstName':'Зарина','phone':'+996706789012','email':'zarina@example.com','region':'Таласская область','role':'user','rating':4.7,'reviewCount':2,'listingCount':5,'avatar':None,'banned':False,'verified':True,'createdAt':1744761600000,'chatIds':{}},
    'user007uid007': {'uid':'user007uid007','name':'Алмаз Сыдыков','firstName':'Алмаз','phone':'+996707890123','email':'almaz@example.com','region':'Баткенская область','role':'user','rating':4.1,'reviewCount':1,'listingCount':5,'avatar':None,'banned':False,'verified':False,'createdAt':1745020800000,'chatIds':{}},
  },
  'listings': {},
  'chats': {},
  'reviews': {},
  'support': {},
  'notifications': {},
  'favorites': {},
}

sellers = {
  'user001uid001': ('Айбек Карыбеков',   '+996701234567', 4.5),
  'user002uid002': ('Гүлнара Исакова',   '+996702345678', 4.8),
  'user003uid003': ('Мирбек Токтосунов', '+996703456789', 4.2),
  'user004uid004': ('Нурзат Асанова',    '+996704567890', 4.6),
  'user005uid005': ('Бекзат Жолдошев',   '+996705678901', 4.3),
  'user006uid006': ('Зарина Мамытова',   '+996706789012', 4.7),
  'user007uid007': ('Алмаз Сыдыков',     '+996707890123', 4.1),
}

listings_raw = [
  ('-Nl001','user001uid001','Скот','Коровы Симментальские, 5 голов','Продаю 5 дойных коров Симментальской породы. Возраст 3-5 лет. Регулярные прививки, ветсвидетельство есть.',85000,True,'Чуйская область',42.70,74.80,1746230400000,{'breed':'Симментальская','quantity':5,'hasVaccinations':True,'chipNumber':'KG-2024-001','passport':{'species':'Корова','breed':'Симментальская','age':4,'sex':'Самка','count':5,'weight':420.0,'condition':'Здоровые','vetCertNo':'ВС-2024-001523','vetDate':'15.03.2026','verified':True,'rejected':False,'sold':False,'vaccines':[{'name':'Ящур','date':'10.01.2026'},{'name':'Бруцеллёз','date':'15.01.2026'}]}}),
  ('-Nl002','user003uid003','Скот','Овцы Алайские, 20 голов','Отара из 20 овец. Возраст 1-3 года. Упитанные, здоровые. Торг при покупке всей отары.',18000,True,'Ошская область',40.50,72.80,1746403200000,{'breed':'Алайская','quantity':20,'hasVaccinations':True,'chipNumber':None,'passport':{'species':'Овца','breed':'Алайская','age':2,'sex':'Самка','count':20,'weight':55.0,'condition':'Хорошее','vetCertNo':'ВС-2024-002845','vetDate':'20.02.2026','verified':True,'rejected':False,'sold':False,'vaccines':[{'name':'Ящур','date':'05.02.2026'}]}}),
  ('-Nl003','user002uid002','Зерно','Пшеница Краснодарская, 5 тонн','Пшеница урожая 2025, влажность 13%. Хранится в сухом складе. Возможна доставка.',22000,False,'Иссык-Кульская область',42.40,76.50,1746576000000,{'weightKg':5000.0,'sort':'Краснодарская','harvestYear':'2025'}),
  ('-Nl004','user001uid001','Овощи','Картофель Невский, 2 тонны','Картофель, урожай 2025. Клубни крупные, ровные. Цена за кг.',35,True,'Чуйская область',42.75,74.90,1746748800000,{'weightKg':2000.0,'sort':'Невский','harvestYear':'2025'}),
  ('-Nl005','user003uid003','Фрукты','Яблоки Апорт, 1 тонна','Свежие яблоки из собственного сада. Крупные, сладкие. Продаю оптом.',60,False,'Ошская область',40.52,72.82,1746921600000,{'weightKg':1000.0,'sort':'Апорт','harvestYear':'2025'}),
  ('-Nl006','user002uid002','Молоко','Коровье молоко свежее, 50 л/день','Домашнее молоко. Жирность 3.8%. Доставка в Чолпон-Ата возможна.',60,False,'Иссык-Кульская область',42.65,77.08,1747094400000,{'volumeLiters':50.0,'fatPercent':3.8,'frequency':'Ежедневно'}),
  ('-Nl007','user001uid001','Птица','Куры-бройлеры, 100 голов','Возраст 45 дней. Вес 2.5-3 кг. Без антибиотиков.',450,False,'Чуйская область',42.86,74.13,1747267200000,{'quantity':100,'ageMonths':2}),
  ('-Nl008','user003uid003','Техника','Трактор МТЗ-82, 2015 год','Отличное состояние. Плуг, культиватор в комплекте. Документы есть.',1500000,True,'Ошская область',40.48,72.79,1747440000000,{}),
  ('-Nl009','user002uid002','Услуги','Вспашка земли трактором','Вспашка по Иссык-Кульской области. Опыт 10 лет. Цена за сотку.',500,True,'Иссык-Кульская область',42.45,76.55,1747612800000,{}),
  ('-Nl010','user001uid001','Корма','Сено люцерновое, 10 тонн','Первый укос 2025. Отличное качество. Доставка по Чуйской долине.',15,False,'Чуйская область',42.78,74.95,1747785600000,{'weightKg':10000.0,'sort':'Люцерна','harvestYear':'2025'}),
  ('-Nl011','user004uid004','Скот','Лошади Кыргызские, 3 головы','Три лошади, возраст 4-6 лет. Объезженные, послушные. Ветсвидетельство есть.',120000,True,'Нарынская область',41.40,75.90,1743465600000,{'breed':'Кыргызская','quantity':3,'hasVaccinations':True,'chipNumber':'KG-2025-112','passport':{'species':'Лошадь','breed':'Кыргызская','age':5,'sex':'Самец','count':3,'weight':380.0,'condition':'Отличное','vetCertNo':'ВС-2025-003014','vetDate':'10.02.2026','verified':True,'rejected':False,'sold':False,'vaccines':[{'name':'Сап','date':'01.02.2026'}]}}),
  ('-Nl012','user004uid004','Молоко','Кумыс свежий, 30 л/день','Из молока кыргызских кобылиц. Традиционный способ. Доставка по Нарыну.',180,False,'Нарынская область',41.42,75.98,1747267200000,{'volumeLiters':30.0,'fatPercent':1.5,'frequency':'Ежедневно'}),
  ('-Nl013','user004uid004','Зерно','Ячмень озимый, 3 тонны','Урожай 2025. Влажность 12%. Подходит для пивоварения и кормления скота.',18000,True,'Нарынская область',41.38,75.85,1744761600000,{'weightKg':3000.0,'sort':'Озимый','harvestYear':'2025'}),
  ('-Nl014','user004uid004','Корма','Силос кукурузный, 15 тонн','Заложен в октябре 2025. Без плесени. Для коров и овец.',10,False,'Нарынская область',41.45,76.02,1746057600000,{'weightKg':15000.0,'sort':'Кукурузный','harvestYear':'2025'}),
  ('-Nl015','user004uid004','Услуги','Зоотехнические консультации','Кормление, разведение, содержание КРС и МРС. Выезд по Нарынской области.',1000,True,'Нарынская область',41.42,75.99,1745539200000,{}),
  ('-Nl016','user005uid005','Фрукты','Персики Ферганские, 300 кг','Сочные персики. Урожай 2025. Подходят для еды и консервации.',120,False,'Джалал-Абадская область',40.85,72.95,1746835200000,{'weightKg':300.0,'sort':'Ферганский','harvestYear':'2025'}),
  ('-Nl017','user005uid005','Фрукты','Грецкий орех, 200 кг','Тонкая скорлупа, полное ядро. Урожай 2025. Оптом.',150,True,'Джалал-Абадская область',40.92,73.02,1745798400000,{'weightKg':200.0,'sort':'Местный','harvestYear':'2025'}),
  ('-Nl018','user005uid005','Зерно','Кукуруза кормовая, 8 тонн','Крупное сухое зерно. Сорт Краснодарский 291. Для откорма скота и птицы.',23000,True,'Джалал-Абадская область',40.90,73.00,1745020800000,{'weightKg':8000.0,'sort':'Краснодарский 291','harvestYear':'2025'}),
  ('-Nl019','user005uid005','Скот','Козы Зааненские, 10 голов','Молочные козы, возраст 2-4 года. Удой до 4 л/день.',12000,True,'Джалал-Абадская область',40.88,72.98,1743724800000,{'breed':'Зааненская','quantity':10,'hasVaccinations':True,'chipNumber':None,'passport':{'species':'Коза','breed':'Зааненская','age':3,'sex':'Самка','count':10,'weight':65.0,'condition':'Хорошее','vetCertNo':'ВС-2025-004177','vetDate':'25.01.2026','verified':True,'rejected':False,'sold':False,'vaccines':[{'name':'Ящур','date':'10.01.2026'}]}}),
  ('-Nl020','user005uid005','Техника','Культиватор КПС-4, 2017 год','Ширина захвата 4 м. Агрегатируется с МТЗ. Документы есть.',80000,True,'Джалал-Абадская область',40.86,72.97,1746316800000,{}),
  ('-Nl021','user006uid006','Зерно','Подсолнечник, 2 тонны','Масличность 48%. Для переработки или кормления птицы.',40000,False,'Таласская область',42.50,72.20,1745280000000,{'weightKg':2000.0,'sort':'Мастер','harvestYear':'2025'}),
  ('-Nl022','user006uid006','Овощи','Лук Центурион, 3 тонны','Головки крупные, плотные. Хорошо хранятся. Продаю оптом.',25,True,'Таласская область',42.52,72.25,1745798400000,{'weightKg':3000.0,'sort':'Центурион','harvestYear':'2025'}),
  ('-Nl023','user006uid006','Птица','Гуси Серые, 20 голов','Возраст 4 мес., вес 4-5 кг. На зерне и траве. Вся партия.',700,True,'Таласская область',42.48,72.18,1745539200000,{'quantity':20,'ageMonths':4}),
  ('-Nl024','user006uid006','Корма','Комбикорм для КРС, 5 тонн','Сбалансированный состав. Фасовка 50 кг. Оптовая цена.',58,True,'Таласская область',42.51,72.22,1745798400000,{'weightKg':5000.0,'sort':'КРС Универсал','harvestYear':'2025'}),
  ('-Nl025','user006uid006','Услуги','Аренда техники для уборки урожая','Комбайн, трактор, прицеп. Аренда посуточно по Таласской области.',5000,True,'Таласская область',42.49,72.21,1746576000000,{}),
  ('-Nl026','user007uid007','Фрукты','Абрикосы Исфаринские, 500 кг','Крупные, сладкие. Урожай июня 2025. Доставка в Ош возможна.',90,True,'Баткенская область',39.75,70.55,1746576000000,{'weightKg':500.0,'sort':'Исфаринский','harvestYear':'2025'}),
  ('-Nl027','user007uid007','Фрукты','Гранат, 300 кг','Спелый гранат. Зёрна сочные, насыщенного красного цвета. Урожай 2025.',200,False,'Баткенская область',39.80,70.60,1746835200000,{'weightKg':300.0,'sort':'Баткенский','harvestYear':'2025'}),
  ('-Nl028','user007uid007','Скот','Верблюды Бактрийские, 2 головы','Возраст 5 и 7 лет. Рабочие, обученные. Ветосмотр пройден.',180000,True,'Баткенская область',39.80,70.60,1744502400000,{'breed':'Бактрийский','quantity':2,'hasVaccinations':True,'chipNumber':'KG-2025-205','passport':{'species':'Верблюд','breed':'Бактрийский','age':6,'sex':'Самец','count':2,'weight':550.0,'condition':'Отличное','vetCertNo':'ВС-2025-007634','vetDate':'20.03.2026','verified':True,'rejected':False,'sold':False,'vaccines':[{'name':'Оспа верблюдов','date':'15.03.2026'}]}}),
  ('-Nl029','user007uid007','Услуги','Ветеринарные услуги на выезде','Осмотр, вакцинация, лечение КРС и МРС. Выезд по Баткенской области.',800,True,'Баткенская область',39.77,70.58,1747094400000,{}),
  ('-Nl030','user007uid007','Овощи','Морковь Нантская, 1.5 тонны','Ровные, без повреждений. Для рынка и переработки.',30,False,'Баткенская область',39.82,70.62,1745539200000,{'weightKg':1500.0,'sort':'Нантская','harvestYear':'2025'}),
  ('-Nl031','user006uid006','Скот','Бычки на откорм, 4 головы','Возраст 8-10 мес., вес 180-220 кг. Самовывоз из Таласа.',55000,True,'Таласская область',42.53,72.19,1743984000000,{'breed':'Казахская белоголовая','quantity':4,'hasVaccinations':True,'chipNumber':None,'passport':{'species':'Бык','breed':'Казахская белоголовая','age':1,'sex':'Самец','count':4,'weight':200.0,'condition':'Хорошее','vetCertNo':'ВС-2025-005388','vetDate':'05.03.2026','verified':False,'rejected':False,'sold':False,'vaccines':[{'name':'Ящур','date':'01.03.2026'}]}}),
  ('-Nl032','user005uid005','Птица','Индейки Биг-6, 30 голов','Возраст 3 мес., вес 6-8 кг. Без антибиотиков. Диетическое мясо.',800,True,'Джалал-Абадская область',40.91,73.01,1745020800000,{'quantity':30,'ageMonths':3}),
  ('-Nl033','user002uid002','Овощи','Капуста белокочанная Слава, 2 тонны','Кочаны плотные, 3-5 кг. Урожай 2025. Для квашения.',20,False,'Иссык-Кульская область',42.42,76.60,1746057600000,{'weightKg':2000.0,'sort':'Слава','harvestYear':'2025'}),
  ('-Nl034','user003uid003','Техника','Сеялка зерновая СЗ-3.6, 2018 год','Все диски в норме. Ширина захвата 3.6 м. Подходит для МТЗ-82.',95000,True,'Ошская область',40.46,72.74,1746316800000,{}),
  ('-Nl035','user001uid001','Техника','Косилка роторная КРН-2.1, 2019 год','Ширина 2.1 м. Агрегатируется с МТЗ-80/82. Использовалась 3 сезона.',65000,True,'Чуйская область',42.76,74.92,1746576000000,{}),
  ('-Nl036','user004uid004','Скот','Поросята Ландрас, 15 голов','Возраст 45 дней, вес 8-10 кг. Привиты от чумы свиней.',4500,False,'Нарынская область',41.39,75.92,1744243200000,{'breed':'Ландрас','quantity':15,'hasVaccinations':True,'chipNumber':None,'passport':{'species':'Свинья','breed':'Ландрас','age':0,'sex':'Самец','count':15,'weight':9.0,'condition':'Здоровые','vetCertNo':'ВС-2025-006521','vetDate':'12.03.2026','verified':True,'rejected':False,'sold':False,'vaccines':[{'name':'Классическая чума свиней','date':'10.03.2026'}]}}),
  ('-Nl037','user007uid007','Молоко','Козье молоко, 15 л/день','Жирность 4.2%. Горные пастбища. Для детей полезно.',100,False,'Баткенская область',40.58,70.62,1747440000000,{'volumeLiters':15.0,'fatPercent':4.2,'frequency':'Ежедневно'}),
  ('-Nl038','user002uid002','Птица','Утки Пекинские, 50 голов','Возраст 2 мес., вес 2.5-3 кг. Выращены в чистых условиях.',500,False,'Иссык-Кульская область',42.60,77.00,1745280000000,{'quantity':50,'ageMonths':2}),
  ('-Nl039','user006uid006','Фрукты','Виноград Кишмиш белый, 400 кг','Без косточек. Грозди крупные, ягода сладкая. Собственный виноградник.',150,True,'Таласская область',42.46,72.17,1747094400000,{'weightKg':400.0,'sort':'Кишмиш белый','harvestYear':'2025'}),
  ('-Nl040','user003uid003','Услуги','Перевозка скота по Кыргызстану','Скотовоз. Маршруты: Ош-Бишкек-Иссык-Куль-Нарын. Опыт 8 лет.',3000,True,'Ошская область',40.51,72.80,1747267200000,{}),
]

for lid, uid, cat, title, desc, price, neg, region, lat, lng, ts, extra in listings_raw:
    sname, sphone, srating = sellers[uid]
    obj = {
      'id': lid, 'uid': uid, 'category': cat, 'title': title, 'description': desc,
      'price': float(price), 'negotiable': neg, 'region': region,
      'latitude': lat, 'longitude': lng, 'photos': [P[cat]],
      'active': True, 'pending': False, 'rejected': False, 'sold': False,
      'rejectReason': None, 'createdAt': ts,
      'sellerName': sname, 'sellerPhone': sphone, 'sellerAvatar': None, 'sellerRating': srating,
    }
    obj.update(extra)
    data['listings'][lid] = obj

data['listings']['-Nl010']['active'] = False
data['listings']['-Nl010']['pending'] = True

data['chats'] = {
  '-Nchat001chatId00': {
    'participants': {'user001uid001': True, 'user002uid002': True},
    'listingId': '-Nl001', 'updatedAt': 1747875600000,
    'lastMessage': 'Хорошо, жду вас завтра утром', 'lastSenderId': 'user001uid001',
    'unread': {'user001uid001': 0, 'user002uid002': 1},
    'messages': {
      '-Nm001a': {'senderId':'user002uid002','text':'Здравствуйте! Коровы ещё в продаже?','createdAt':1747872000000,'type':'text'},
      '-Nm001b': {'senderId':'user001uid001','text':'Да, осталось 5 голов.','createdAt':1747872600000,'type':'text'},
      '-Nm001c': {'senderId':'user002uid002','text':'Могу приехать в выходные посмотреть вживую?','createdAt':1747873200000,'type':'text'},
      '-Nm001d': {'senderId':'user001uid001','text':'Конечно! Суббота с 9:00, с. Новопавловка.','createdAt':1747873800000,'type':'text'},
      '-Nm001e': {'senderId':'user002uid002','text':'Если возьму всех 5 — скидку дадите?','createdAt':1747874400000,'type':'text'},
      '-Nm001f': {'senderId':'user001uid001','text':'Скидку 5% сделаю. Итого 403 750 сом.','createdAt':1747875000000,'type':'text'},
      '-Nm001g': {'senderId':'user002uid002','text':'Договорились! Приеду в субботу.','createdAt':1747875300000,'type':'text'},
      '-Nm001h': {'senderId':'user001uid001','text':'Хорошо, жду вас завтра утром','createdAt':1747875600000,'type':'text'},
    }
  },
  '-Nchat002chatId00': {
    'participants': {'user001uid001': True, 'user003uid003': True},
    'listingId': '-Nl003', 'updatedAt': 1747958400000,
    'lastMessage': 'Позвоню перед отъездом.', 'lastSenderId': 'user001uid001',
    'unread': {'user001uid001': 0, 'user003uid003': 0},
    'messages': {
      '-Nm002a': {'senderId':'user001uid001','text':'Добрый день! Пшеница ещё есть?','createdAt':1747957800000,'type':'text'},
      '-Nm002b': {'senderId':'user003uid003','text':'Да, есть. Сколько нужно?','createdAt':1747957900000,'type':'text'},
      '-Nm002c': {'senderId':'user001uid001','text':'Тонна. Доставка в Бишкек возможна?','createdAt':1747958000000,'type':'text'},
      '-Nm002d': {'senderId':'user003uid003','text':'Возможна, +5000 сом за доставку.','createdAt':1747958200000,'type':'text'},
      '-Nm002e': {'senderId':'user001uid001','text':'Позвоню перед отъездом.','createdAt':1747958400000,'type':'text'},
    }
  },
  '-Nchat003chatId00': {
    'participants': {'user002uid002': True, 'user004uid004': True},
    'listingId': '-Nl012', 'updatedAt': 1747960000000,
    'lastMessage': 'Жду вашего звонка!', 'lastSenderId': 'user004uid004',
    'unread': {'user002uid002': 2, 'user004uid004': 0},
    'messages': {
      '-Nm003a': {'senderId':'user002uid002','text':'Здравствуйте, кумыс свежий есть сейчас?','createdAt':1747959000000,'type':'text'},
      '-Nm003b': {'senderId':'user004uid004','text':'Есть, готовим каждый день!','createdAt':1747959300000,'type':'text'},
      '-Nm003c': {'senderId':'user002uid002','text':'Сколько литров можете давать ежедневно?','createdAt':1747959600000,'type':'text'},
      '-Nm003d': {'senderId':'user004uid004','text':'До 30 литров. 180 сом/литр.','createdAt':1747959800000,'type':'text'},
      '-Nm003e': {'senderId':'user002uid002','text':'Можно оформить еженедельную поставку?','createdAt':1747959900000,'type':'text'},
      '-Nm003f': {'senderId':'user004uid004','text':'Конечно! Жду вашего звонка!','createdAt':1747960000000,'type':'text'},
    }
  },
}

data['reviews'] = {
  'user001uid001': {
    'user002uid002': {'authorName':'Гүлнара Исакова','authorUid':'user002uid002','rating':5.0,'text':'Отличный продавец! Коровы как описано.','listingId':'-Nl001','createdAt':1747958000000},
    'user003uid003': {'authorName':'Мирбек Токтосунов','authorUid':'user003uid003','rating':4.0,'text':'Хороший продавец. Небольшая задержка, но доволен.','listingId':'-Nl004','createdAt':1747440000000},
  },
  'user002uid002': {
    'user004uid004': {'authorName':'Нурзат Асанова','authorUid':'user004uid004','rating':5.0,'text':'Молоко отличного качества, всегда свежее!','listingId':'-Nl006','createdAt':1747267200000},
    'user003uid003': {'authorName':'Мирбек Токтосунов','authorUid':'user003uid003','rating':5.0,'text':'Пшеница хорошего качества, всё как описано.','listingId':'-Nl003','createdAt':1746576000000},
    'user001uid001': {'authorName':'Айбек Карыбеков','authorUid':'user001uid001','rating':4.0,'text':'Хорошее молоко, рекомендую!','listingId':'-Nl006','createdAt':1747300000000},
  },
  'user003uid003': {
    'user001uid001': {'authorName':'Айбек Карыбеков','authorUid':'user001uid001','rating':5.0,'text':'Яблоки свежие, качество отличное!','listingId':'-Nl005','createdAt':1746921600000},
    'user005uid005': {'authorName':'Бекзат Жолдошев','authorUid':'user005uid005','rating':4.0,'text':'Трактор в хорошем состоянии.','listingId':'-Nl008','createdAt':1747440000000},
  },
  'user004uid004': {
    'user002uid002': {'authorName':'Гүлнара Исакова','authorUid':'user002uid002','rating':5.0,'text':'Кумыс настоящий, вкусный! Всегда вовремя.','listingId':'-Nl012','createdAt':1747960000000},
  },
  'user007uid007': {
    'user006uid006': {'authorName':'Зарина Мамытова','authorUid':'user006uid006','rating':4.0,'text':'Абрикосы хорошие. Чуть кислее чем ожидала.','listingId':'-Nl026','createdAt':1746700000000},
  },
}

data['support'] = {
  '-Nticket001': {'userId':'user001uid001','userName':'Айбек Карыбеков','subject':'Объявления','message':'Объявление о сене на проверке 2 дня. Когда будет опубликовано?','createdAt':1747958400000,'status':'open','replies':{}},
  '-Nticket002': {'userId':'user002uid002','userName':'Гүлнара Исакова','subject':'Аккаунт','message':'Как пройти верификацию? Профиль заполнила полностью.','createdAt':1747872000000,'status':'answered','replies':{'-Nr002a':{'fromAdmin':True,'text':'Верификация выполнена вручную — получите статус в течение 24 часов. Спасибо!','createdAt':1747958400000}}},
  '-Nticket003': {'userId':'user003uid003','userName':'Мирбек Токтосунов','subject':'Паспорт животного','message':'Паспорт овец не проходит верификацию. Документы в порядке.','createdAt':1747785600000,'status':'open','replies':{}},
  '-Nticket004': {'userId':'user005uid005','userName':'Бекзат Жолдошев','subject':'Чаты','message':'Не могу отправить сообщение продавцу — выдаёт ошибку.','createdAt':1747900000000,'status':'open','replies':{}},
  '-Nticket005': {'userId':'user007uid007','userName':'Алмаз Сыдыков','subject':'Общий вопрос','message':'Как добавить несколько фотографий к объявлению?','createdAt':1747850000000,'status':'answered','replies':{'-Nr005a':{'fromAdmin':True,'text':'При создании объявления нажмите «Добавить фото» — можно добавить до 5 фотографий.','createdAt':1747860000000}}},
}

data['notifications'] = {
  'user001uid001': {
    '-Nnot1a': {'title':'Объявление опубликовано','message':'«Коровы Симментальские» опубликовано.','type':'listing_approved','listingId':'-Nl001','ticketId':None,'createdAt':1746316800000,'read':True},
    '-Nnot1b': {'title':'Новое сообщение','message':'Гүлнара Исакова написала вам в чате.','type':'new_message','listingId':'-Nl001','ticketId':None,'createdAt':1747872000000,'read':False},
  },
  'user002uid002': {
    '-Nnot2a': {'title':'Объявление опубликовано','message':'«Пшеница 2025 года» опубликовано.','type':'listing_approved','listingId':'-Nl003','ticketId':None,'createdAt':1746662400000,'read':True},
    '-Nnot2b': {'title':'Ответ на обращение','message':'Администратор ответил на обращение «Аккаунт».','type':'support_reply','listingId':None,'ticketId':'-Nticket002','createdAt':1747958400000,'read':False},
    '-Nnot2c': {'title':'Новое сообщение','message':'Нурзат Асанова написала вам в чате.','type':'new_message','listingId':'-Nl012','ticketId':None,'createdAt':1747959000000,'read':False},
  },
  'user003uid003': {
    '-Nnot3a': {'title':'Объявление отклонено','message':'«Яблоки Апорт» отклонено: нужны фотографии.','type':'listing_rejected','listingId':'-Nl005','ticketId':None,'createdAt':1747008000000,'read':True},
    '-Nnot3b': {'title':'Объявление опубликовано','message':'«Яблоки Апорт» опубликовано.','type':'listing_approved','listingId':'-Nl005','ticketId':None,'createdAt':1747094400000,'read':True},
  },
  'user004uid004': {
    '-Nnot4a': {'title':'Паспорт верифицирован','message':'Паспорт лошадей успешно проверен.','type':'passport_verified','listingId':'-Nl011','ticketId':None,'createdAt':1743638400000,'read':False},
  },
  'user007uid007': {
    '-Nnot7a': {'title':'Ответ на обращение','message':'Администратор ответил на обращение «Общий вопрос».','type':'support_reply','listingId':None,'ticketId':'-Nticket005','createdAt':1747860000000,'read':False},
  },
}

data['favorites'] = {
  'user001uid001': {'-Nl003':True,'-Nl008':True,'-Nl026':True},
  'user002uid002': {'-Nl001':True,'-Nl011':True,'-Nl028':True},
  'user003uid003': {'-Nl004':True,'-Nl006':True,'-Nl034':True},
  'user004uid004': {'-Nl001':True,'-Nl003':True},
  'user005uid005': {'-Nl016':True,'-Nl039':True},
  'user006uid006': {'-Nl028':True,'-Nl011':True},
  'user007uid007': {'-Nl026':True,'-Nl027':True},
}

with open('seed_data.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print(f"Done. Listings: {len(data['listings'])}, Users: {len(data['users'])}")
sz = sum(len(str(v.get('photos',[''])[0])) for v in data['listings'].values() if v.get('photos'))
print(f"Total photos base64 size: ~{sz//1024} KB")
