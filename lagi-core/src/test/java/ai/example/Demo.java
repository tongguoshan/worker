package ai.example;

import ai.audio.service.AudioService;
import ai.common.pojo.*;
import ai.config.ContextLoader;
import ai.image.adapter.IImageGenerationAdapter;
import ai.image.pojo.ImageEnhanceRequest;
import ai.image.service.AllImageService;
import ai.image.service.ImageGenerationService;
import ai.llm.service.CompletionsService;
import ai.openai.pojo.ChatCompletionChoice;
import ai.openai.pojo.ChatCompletionRequest;
import ai.openai.pojo.ChatCompletionResult;
import ai.openai.pojo.ChatMessage;
import ai.video.pojo.*;
import ai.video.service.AllVideoService;
import com.google.common.collect.Lists;
import io.reactivex.Observable;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;


public class Demo {

    static {
        //initialize Profiles
        ContextLoader.loadContext();
    }

    /**
     * Demonstration function for testing the chat completion feature.
     * This function initializes the environment, constructs a mock chat completion request, calls the completions method of the CompletionsService class,
     * and prints the content of the first completion result.
     */

    public static void chat() {

        //mock request
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
        chatCompletionRequest.setTemperature(0.8);
        chatCompletionRequest.setMax_tokens(1024);
        chatCompletionRequest.setCategory("default");
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent("你好");
        chatCompletionRequest.setMessages(Lists.newArrayList(message));
        // Set the stream parameter to false
        chatCompletionRequest.setStream(false);
        // Create an instance of CompletionsService
        CompletionsService completionsService = new CompletionsService();
        // Call the completions method to process the chat completion request
        ChatCompletionResult result = completionsService.completions(chatCompletionRequest);

        // Print the content of the first completion choice
        System.out.println("outcome:" + result.getChoices().get(0).getMessage().getContent());

    }

    /**
     * Demonstration function for chat completion.
     * Initializes and sends a chat completion request, then processes and outputs the completion results through an observable subscription.
     */

    public static void streamChat() {
        // Initialize the chat completion request
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
        chatCompletionRequest.setTemperature(0.8);
        chatCompletionRequest.setMax_tokens(1024);
        chatCompletionRequest.setCategory("default");
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent("你好");
        chatCompletionRequest.setMessages(Lists.newArrayList(message));
        // Create a CompletionsService instance for sending completion requests
        CompletionsService completionsService = new CompletionsService();
        // Set the chat completion request to stream mode
        chatCompletionRequest.setStream(true);
        // Send the request and get an observable for streaming results
        Observable<ChatCompletionResult> observable = completionsService.streamCompletions(chatCompletionRequest);
        // Use an array of size 2 to store the last two result states
        final ChatCompletionResult[] lastResult = {null, null};
        observable.subscribe(
                // Update the latest result
                data -> {
                    lastResult[0] = data;
                    // If the second latest result is null, initialize it with the latest resu
                    if (lastResult[1] == null) {
                        lastResult[1] = data;
                    } else {
                        // Merge the content of the same choice index in the latest two results
                        for (int i = 0; i < lastResult[1].getChoices().size(); i++) {
                            ChatCompletionChoice choice = lastResult[1].getChoices().get(i);
                            ChatCompletionChoice chunkChoice = data.getChoices().get(i);
                            // Merge the content
                            String chunkContent = chunkChoice.getMessage().getContent();
                            String content = choice.getMessage().getContent();
                            choice.getMessage().setContent(content + chunkContent);
                            // Output the merged content
                            System.out.println("outcome" + content);
                        }
                    }
                }
        );
    }

    /**
     * ASR Demo Method
     * This method demonstrates how to utilize the speech service for audio recognition.
     * It starts by specifying a string containing the path to the audio file, then creates
     * an audio request parameter object and an audio service object.
     * Subsequently, it uses the recognition method of the audio service object to identify
     * the audio file and prints the recognition result.
     */
    public static void asrAudio(){
        // Specify the path to the audio file
        String resPath = "C:\\temp\\audiofile.wav";
        // Create an audio request parameter object
        AudioRequestParam param = new AudioRequestParam();
        // Instantiate the audio service object
        AudioService audioService = new AudioService();
        // Perform audio recognition using the speech service
        AsrResult result = audioService.asr(resPath, param);
        // Print the recognition result
        System.out.println("outcome:" +result);
    }

    public static void Text2Audio(){
        TTSRequestParam request = new TTSRequestParam();
        request.setText("你好");
        AudioService audioService = new AudioService();
        TTSResult result = audioService.tts(request);
        System.out.println("outcome" +result);
    }

    /**
     * Generates a landscape map image.
     * This method creates an image generation request, specifies the prompt for the map generation,
     * calls the image service to perform the generation, and outputs the result.
     * The purpose is to demonstrate how to use the image generation service to create a specific type of image.
     */
    public static void generationsImage() {
        // Create an image generation request object
        ImageGenerationRequest request = new ImageGenerationRequest();
        // Set the prompt for the image generation, specifying the desired landscape map
        request.setPrompt("Help me generate a landscape map");

        // Create an instance of the image service to handle the image generation request
        AllImageService imageService = new AllImageService();
        // Call the image service to perform the generation, passing in the request and receiving the result
        ImageGenerationResult result = imageService.generations(request);

        // Output the result of the image generation
        System.out.println("outcome:" +result.getData());
    }

    /**
     * Converts an image into text description.
     * This method utilizes an image processing service to convert the image located at a specified path into a textual description.
     * Primarily used for translating visual content into descriptive text, such as identifying objects or scenes within the image.
     */
    public static void Image2Text() {
        // Specifies the path of the image file to be converted
        // YOUR IMAGE URL
        String lastImageFile = "C:\\temp\\th.jpg";
        // Instantiates the image processing service
        AllImageService allImageService = new AllImageService();
        // Creates a File object based on the file path
        File file = new File(lastImageFile);
        // Invokes the service to convert the image into text description
        ImageToTextResponse text = allImageService.toText(FileRequest.builder().imageUrl(file.getAbsolutePath()).build());
        // Outputs the converted text description
        System.out.println("outcome:" +text.getCaption());
    }

    public static void trackVideo() {
        String lastVideoFile = "https://abc12345abc.oss-cn-hangzhou.aliyuncs.com/a8345c6f036787646fe807f9bfff7870.mp4";
        AllVideoService videoService = new AllVideoService();
        VideoTackRequest videoTackRequest = VideoTackRequest.builder().videoUrl(lastVideoFile).build();
        VideoJobResponse track = videoService.track(videoTackRequest);
        System.out.println("outcome:" +track.getData());
    }

    /**
     * Enhances the quality of an image.
     * This method invokes an image enhancement service to improve the quality of the specified image URL.
     * It is primarily used to enhance image clarity and color representation, suitable for image processing scenarios.
     */
    public static void enhanceImage() {
        // Set the URL of the image to be processed
        String imageUrl = "https://abc12345abc.oss-cn-hangzhou.aliyuncs.com/a.png";
        // Instantiate the image enhancement service
        AllImageService allImageService = new AllImageService();
        // Build the image enhancement request with the specified image URL
        ImageEnhanceRequest imageEnhanceRequest = ImageEnhanceRequest.builder().imageUrl(imageUrl).build();
        // Invoke the image enhancement service to process the request
        ImageEnhanceResult enhance = allImageService.enhance(imageEnhanceRequest);
        // Output the processing result
        System.out.println("outcome:" + enhance);
    }

    /**
     * Converts an image into a video. This method demonstrates the process of
     * transforming a single image into a video by invoking the image2Video method
     * from the AllVideoService class. Primarily used as an example to showcase
     * image-to-video conversion capabilities.
     */
    public static void image2Video() {
        // Specifies the path of the image to be converted
        String imageUrl = "C:\\temp\\th.jpg";
        // Instantiates AllVideoService to access video generation services
        AllVideoService allVideoService = new AllVideoService();

        // Constructs the request for video generation, specifying details of the input image
        VideoGeneratorRequest videoGeneratorRequest = VideoGeneratorRequest.builder()
                .inputFileList(Collections.singletonList(InputFile.builder().url(imageUrl).name("th").type("jpg").build()))
                .build();

        // Submits the request to convert the image into a video via the service interface
        VideoJobResponse videoGenerationResult = allVideoService.image2Video(videoGeneratorRequest);

        // Outputs the data portion of the generation result
        System.out.println("outcome:" + videoGenerationResult.getData());
    }

    /**
     * A static method to enhance the quality of a video.
     * This method utilizes the enhance function from AllVideoService to process and improve the quality of a specified video file,
     * primarily serving as a demonstration of video enhancement capabilities.
     */
    public static void enhanceVideo(){
        // Specify the path of the video file to be processed
        String lastVideoFile = "https://abc12345abc.oss-cn-hangzhou.aliyuncs.com/a8345c6f036787646fe807f9bfff7870.mp4";
        // Instantiate AllVideoService to invoke video enhancement features
        AllVideoService allVideoService = new  AllVideoService();

        // Build a video enhancement request object with the video URL set
        VideoEnhanceRequest videoEnhanceRequest = new VideoEnhanceRequest();
        videoEnhanceRequest.setVideoURL(lastVideoFile);
        // Invoke the video enhancement service, passing the request object, and receive the processing result
        VideoJobResponse videoGenerationResult = allVideoService.enhance(videoEnhanceRequest);
        // Output the data portion of the processing result
        System.out.println("outcome:" + videoGenerationResult.getData());
    }

    public static void main(String[] args) {
        //completions example
        // chat();

        //streamCompletions example
        //streamChat();

        //asr example
        //asrAudio();

        //tts example
        //ttsAudio();

        //generations example
        //generationsImage();

        //Image2Text example
        //Image2Text();

        //track example
        trackVideo();

        //Image enhance example
        //enhanceImage();

        //image2Video example
        //image2Video();

        //enhanceVideo example
        //enhanceVideo();

    }


}
