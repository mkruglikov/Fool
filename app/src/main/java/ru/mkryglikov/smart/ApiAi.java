package ru.mkryglikov.smart;

import android.os.AsyncTask;

import java.util.concurrent.ExecutionException;

import ai.api.AIDataService;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;

public class ApiAi {
    private final static AIConfiguration config = new AIConfiguration(Config.API_AI_KEY,
            AIConfiguration.SupportedLanguages.Russian,
            AIConfiguration.RecognitionEngine.System);
    private final static AIDataService apiAiService = new AIDataService(config);
    private final static AIRequest apiAiRequest = new AIRequest();

    AIResponse makeRequest(final String query) {
        try {
            return new AsyncTask<Void, Void, AIResponse>() {
                @Override
                protected AIResponse doInBackground(Void... params) {
                    apiAiRequest.setQuery(query);
                    try {
                        return apiAiService.request(apiAiRequest);
                    } catch (AIServiceException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }
}
