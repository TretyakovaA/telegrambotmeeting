package org.example.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import org.example.model.Event;
import org.example.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramUpdateHandler {

    private final Logger logger = LoggerFactory.getLogger(TelegramUpdateHandler.class);

    private final TelegramBot telegramBot;

    private int menuNumber = -1;

    private String tempEventName;
    private LocalDateTime eventDate;
    private String tempEventLink;

    //меню0
    public final String MENU0_BUTTON1 = "Посмотреть анонсы мероприятий";
    public final String MENU0_BUTTON2 = "Добавить свое мероприятие";

    //меню1
    public final String MENU1_BUTTON1 = "Предпросмотр";
    public final String MENU1_BUTTON2 = "Сохранить";
    public final String MENU1_BUTTON3 = "Очистить";

    //меню 11-13
    public final String MENU10_BUTTON0 = "Вернуться к предыдущему пункту";

    private Pattern pattern = Pattern.compile("([0-9\\.\\:\\s]{16})");

    private final EventRepository eventRepository;

    public TelegramUpdateHandler(TelegramBot telegramBot, EventRepository eventRepository) {
        this.telegramBot = telegramBot;
        this.eventRepository = eventRepository;
    }

    public void showMenu(long chatId) {
        System.out.println(chatId);
        switch (menuNumber) {
            case 11:
                telegramBot.execute(new SendMessage(chatId, "Введите название мероприятия")
                        .replyMarkup(new ReplyKeyboardMarkup(new String[]{MENU10_BUTTON0})));
                break;
            case 12:
                telegramBot.execute(new SendMessage(chatId, "Введите дату и время мероприятия в формате: дд.мм.гггг чч:мм")
                        .replyMarkup(new ReplyKeyboardMarkup(new String[]{MENU10_BUTTON0})));
                break;
            case 13:
                telegramBot.execute(new SendMessage(chatId, "Пришлите ссылку на мероприятие")
                        .replyMarkup(new ReplyKeyboardMarkup(new String[]{MENU10_BUTTON0})));
                break;
            case 0:
                telegramBot.execute(new SendMessage(chatId, "Что вы хотите сделать?")
                        .replyMarkup(new ReplyKeyboardMarkup(new String[]{MENU0_BUTTON1}, new String[]{MENU0_BUTTON2})));
                menuNumber = 0;
                break;
            case 1:
                telegramBot.execute(new SendMessage(chatId, "Выберете действие ")
                        .replyMarkup(new ReplyKeyboardMarkup(new String[]{MENU1_BUTTON1},
                                new String[]{MENU1_BUTTON2}, new String[]{MENU1_BUTTON3})));
                break;
        }

    }

    /**
     * Фасад, основная логика построения меню бота и обработки статусов
     * По окончанию метода, отправляем сообщение или редактируем, сохраняем ответ полученный от телеги
     *
     * @param update Update
     */
    public void processUpdate(Update update) throws IOException {
        logger.info("Processing update: {}", update);
        // Process your updates here
        long chatId = update.message().chat().id();
        String userAnswer = update.message().text();

        if (userAnswer.equals("/start")) {
            telegramBot.execute(new SendMessage(chatId, "Здравствуйте!"));
            menuNumber = 0;
            showMenu(chatId);
        } else if (menuNumber == -1) {
            menuNumber = 0;
            showMenu(chatId);
        } else if (menuNumber == 0) {
            switch (userAnswer) {
                // Посмотреть анонсы мероприятий
                case MENU0_BUTTON1:
                    List<Event> allEvents = eventRepository.findAll();
                    for (Event event : allEvents){
                        telegramBot.execute(new SendMessage(chatId, event.getName() + " " + event.getEventDate() + " " + event.getLink()));
                    }
                    menuNumber = 0;
                    showMenu(chatId);
                    break;
                // добавить свое мероприятие
                case MENU0_BUTTON2:
                    menuNumber = 11;
                    showMenu(chatId);
                    break;
            }
        } else if (menuNumber == 11) {
            switch (userAnswer) {
                case MENU10_BUTTON0:
                    menuNumber = 0;
                    showMenu(chatId);
                    break;
                default:
                    tempEventName = userAnswer;
                    menuNumber = 12;
                    showMenu(chatId);
            }
        } else if (menuNumber == 12) {
            switch (userAnswer) {
                case MENU10_BUTTON0:
                    menuNumber = 11;
                    showMenu(chatId);
                    break;
                default:
                    Matcher matcher = pattern.matcher(userAnswer);
                    if (matcher.matches()) {
                        String date = matcher.group(1);
                        eventDate = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                        menuNumber = 13;
                        showMenu(chatId);
                    } else {
                        telegramBot.execute(new SendMessage(chatId, " Ошибка! Введите дату и время в формате: дд.мм.гггг чч:мм"));
                        menuNumber = 12;
                        showMenu(chatId);
                    }
            }
        } else if (menuNumber == 13) {
            switch (userAnswer) {
                case MENU10_BUTTON0:
                    menuNumber = 12;
                    showMenu(chatId);
                    break;
                default:
                    tempEventLink = userAnswer;
                    menuNumber = 1;
                    showMenu(chatId);
            }
        } else if (menuNumber == 1) {
            switch (userAnswer) {
                //Предпросмотр
                case MENU1_BUTTON1:
                    telegramBot.execute(new SendMessage(chatId, tempEventName + " " + eventDate + " " + tempEventLink));
                    menuNumber = 1;
                    showMenu(chatId);
                    break;
                //Сохранить
                case MENU1_BUTTON2:
                    //сохраняем в базу мероприятие
                    Event myEvent = new Event();
                    myEvent.setEventDate(eventDate);
                    myEvent.setName(tempEventName);
                    myEvent.setLink(tempEventLink);
                    myEvent.setCreator(chatId);
                    eventRepository.save(myEvent);
                    logger.info("New event's added by ", chatId + " : " + tempEventName + " " + eventDate + " " + tempEventLink);
                    //обнулить название, дату и ссылку
                    eventDate = null;
                    tempEventName = null;
                    tempEventLink = null;
                    telegramBot.execute(new SendMessage(chatId, "Мероприятие успешно сохранено"));
                    menuNumber = 0;
                    showMenu(chatId);
                    break;
                //Очистить
                case MENU1_BUTTON3:
                    //обнулить название, дату и ссылку
                    eventDate = null;
                    tempEventName = null;
                    tempEventLink = null;
                    telegramBot.execute(new SendMessage(chatId, "Мероприятие не сохранено"));
                    menuNumber = 0;
                    showMenu(chatId);
                    break;
            }
        }
    }

    /*//тестовый раз в две минуты
    @Scheduled(cron = "0 0/2 * * * *")
    //ежедневно в 12 дня
    //@Scheduled(cron = "0 0 12 * * *")
    public void testDailyMail() {
    }*/

}
