package com.san.dataflow;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.PipelineRunner;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

public class ReadFromS3 {

    private static final Logger log = LoggerFactory.getLogger(ReadFromS3.class);

    public static void main(String[] args) {
        System.out.println("READ FROM S3, args " + Arrays.toString(args));
        Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
        Pipeline pipeline = Pipeline.create(options);
        try {
            options.setRunner((Class<? extends PipelineRunner<?>>)
                    Class.forName("org.apache.nemo.client.beam.NemoRunner"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        AWSCredentials awsCredentials = new BasicAWSCredentials(options.getAWSAccessKey().get(),
                options.getAWSSecretKey().get());
        options.setAwsCredentialsProvider(new AWSStaticCredentialsProvider(awsCredentials));

        PCollection<String> fileLines =
                pipeline.apply("ReadFromFile", TextIO.read().from("s3://lambda-executor-examples/sample1.csv")); //Replace the file path

        fileLines.apply("PrintLines", MapElements.via(new SimpleFunction<String, String>() {
            @Override
            public String apply(String lines) {
                System.out.println(lines);
                return lines;
            }
        })).apply("WriteToS3", TextIO.write().to("s3://lambda-executor-examples/sample_2.csv"));

        PipelineResult result = pipeline.run();
        try {
            result.getState(); // To skip the error while creating the template
            result.waitUntilFinish();
        } catch (UnsupportedOperationException e) {
            log.error("UnsupportedOperationException :" + e.getMessage());
        } catch (Exception e) {
            log.error("Exception :" + e.getMessage(), e);
        }
    }
}
