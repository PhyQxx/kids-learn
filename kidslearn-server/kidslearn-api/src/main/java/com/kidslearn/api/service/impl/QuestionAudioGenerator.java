package com.kidslearn.api.service.impl;

import java.io.IOException;
import java.nio.file.Path;

public interface QuestionAudioGenerator {
    Path generate(String text) throws IOException;
}
