import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RandomDogImageFetcher {

    private static final String API_KEY = "your_api_key ";
    private static final int CONNECT_TIMEOUT = 10; // seconds
    private static final int READ_TIMEOUT = 15; // seconds
    private static final int MAX_RETRIES = 3;

    public static dogImage getRandomdogImage() throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(new RetryInterceptor(MAX_RETRIES))
                .build();

        HttpUrl apiUrl = HttpUrl.parse("https://api.thedogapi.com/v1/images/search")
                .newBuilder()
                .addQueryParameter("api_key", API_KEY)
                .build();

        Request request = new Request.Builder()
                .url(apiUrl)
                .build();

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            String responseBody = response.body().string();
            ObjectMapper objectMapper = new ObjectMapper();
            dogImage[] dogImages = objectMapper.readValue(responseBody, dogImage[].class);
            if (dogImages.length > 0) {
                return dogImages[0];
            }
        }

        return null;
    }

    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;
        private int retryCount = 0;

        RetryInterceptor(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request);

            while (!response.isSuccessful() && retryCount < maxRetries) {
                retryCount++;
                response = chain.proceed(request);
            }

            return response;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class dogImage {
        @JsonProperty("id")
        private String id;

        @JsonProperty("url")
        private String url;

        public String getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }
    }
}
