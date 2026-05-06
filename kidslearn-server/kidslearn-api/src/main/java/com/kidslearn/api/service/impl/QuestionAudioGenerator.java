package com.kidslearn.api.service.impl;

import java.io.IOException;
import java.nio.file.Path;

interface QuestionAudioGenerator {
    Path generate(String text) throws IOException;
}
