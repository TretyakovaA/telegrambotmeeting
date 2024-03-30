package pro.sky.maternity.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.sky.maternity.mapper.MaternityHospitalDtoMapper;
import pro.sky.maternity.repository.MaternityHospitalRepository;

import javax.annotation.PostConstruct;
import java.util.List;


@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);


    private final TelegramBot telegramBot;
    private final TelegramUpdateHandler telegramUpdateHandler;


    public TelegramBotUpdatesListener(TelegramBot telegramBot, MaternityHospitalRepository maternityHospitalRepository, MaternityHospitalDtoMapper maternityHospitalDtoMapper, TelegramUpdateHandler telegramUpdateHandler) {
        this.telegramBot = telegramBot;
        this.telegramUpdateHandler = telegramUpdateHandler;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            try {
                telegramUpdateHandler.processUpdate(update);
            } catch (Exception exception) {
                logger.info("exception: {}", exception.getMessage());
            }

        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
}
