import okhttp3.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {

    private static final String BOT_USERNAME = "your_username";
    private static final String BOT_TOKEN = "your_token";
    private static final String API_KEY = "your_api_key";

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            String messageText = update.getMessage().getText().toLowerCase();
            if (messageText.equals("/dog")) {
                try {
                    sendRandomdogMessage(update.getMessage().getChatId());
                } catch (IOException | TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            String data = callbackQuery.getData();
            Long chatId = callbackQuery.getMessage().getChatId();

            if (data.startsWith("vote_")) {
                String[] parts = data.split("_");
                String imageId = parts[1];
                int value = Integer.parseInt(parts[2]);

                try {
                    createVote(imageId, value);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Delete the asking message
                deleteMessage(chatId, callbackQuery.getMessage().getMessageId());

                if (value == 1 || value == -1) {
                    // Send a new dog image along with the asking message
                    try {
                        sendRandomdogMessage(chatId);
                    } catch (IOException | TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            } else if (data.equals("stop")) {
                // Delete the asking message
                deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
            } else if (data.equals("yes") || data.equals("no")) {
                // Vote Yes or No and then stop
                String imageId = callbackQuery.getMessage().getPhoto().get(0).getFileId();
                int value = data.equals("yes") ? 1 : -1;

                try {
                    createVote(imageId, value);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Delete the asking message
                deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
            }
        }
    }

    private void createVote(String imageId, int value) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String apiUrl = "https://api.thedogapi.com/v1/votes";

        RequestBody requestBody = new FormBody.Builder()
                .add("image_id", imageId)
                .add("value", String.valueOf(value))
                .build();

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("x-api-key", API_KEY)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        response.close();
    }

    private void sendRandomdogMessage(Long chatId) throws IOException, TelegramApiException {
        RandomDogImageFetcher.dogImage dogImage = RandomDogImageFetcher.getRandomdogImage();

        if (dogImage != null) {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setPhoto(new InputFile(dogImage.getUrl()));
            execute(sendPhoto);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Here's a random dog image! Do you like it?");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            List<InlineKeyboardButton> voteRow = new ArrayList<>();
            InlineKeyboardButton upvoteButton = new InlineKeyboardButton();
            upvoteButton.setText("Yes & Send New dog");
            upvoteButton.setCallbackData("vote_" + dogImage.getId() + "_1");
            voteRow.add(upvoteButton);

            InlineKeyboardButton downvoteButton = new InlineKeyboardButton();
            downvoteButton.setText("No & Send New dog");
            downvoteButton.setCallbackData("vote_" + dogImage.getId() + "_-1");
            voteRow.add(downvoteButton);
            rows.add(voteRow);

            List<InlineKeyboardButton> yesNoRow = new ArrayList<>();
            InlineKeyboardButton yesButton = new InlineKeyboardButton();
            yesButton.setText("Yes");
            yesButton.setCallbackData("yes");
            yesNoRow.add(yesButton);

            InlineKeyboardButton noButton = new InlineKeyboardButton();
            noButton.setText("No");
            noButton.setCallbackData("no");
            yesNoRow.add(noButton);
            rows.add(yesNoRow);

            List<InlineKeyboardButton> stopRow = new ArrayList<>();
            InlineKeyboardButton stopButton = new InlineKeyboardButton();
            stopButton.setText("Stop");
            stopButton.setCallbackData("stop");
            stopRow.add(stopButton);
            rows.add(stopRow);

            markup.setKeyboard(rows);
            message.setReplyMarkup(markup);

            execute(message);
        }
    }

    private void deleteMessage(Long chatId, Integer messageId) {
        try {
            execute(new DeleteMessage(chatId.toString(), messageId));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }
}
