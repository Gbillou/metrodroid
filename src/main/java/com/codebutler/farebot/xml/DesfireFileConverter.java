package com.codebutler.farebot.xml;

import android.util.Base64;

import com.codebutler.farebot.card.desfire.files.DesfireFile;
import com.codebutler.farebot.card.desfire.settings.DesfireFileSettings;
import com.codebutler.farebot.card.desfire.files.InvalidDesfireFile;
import com.codebutler.farebot.card.desfire.files.UnauthorizedDesfireFile;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

public class DesfireFileConverter implements Converter<DesfireFile> {
    private final Serializer mSerializer;

    public DesfireFileConverter(Serializer serializer) {
        mSerializer = serializer;
    }

    @Override public DesfireFile read(InputNode source) throws Exception {
        int id = Integer.parseInt(source.getAttribute("id").getValue());
        InputNode nUnauthorized = source.getAttribute("unauthorized");
        boolean unauthorized = false;
        if (nUnauthorized != null) {
            unauthorized = Boolean.parseBoolean(nUnauthorized.getValue());
        }
        DesfireFileSettings settings = null;
        byte[] data = null;
        String error = null;
        while (true) {
            InputNode node = source.getNext();
            if (node == null) {
                break;
            }
            switch (node.getName()) {
                case "settings":
                    settings = mSerializer.read(DesfireFileSettings.class, node);
                    break;
                case "data":
                    String value = node.getValue();
                    if (value != null) {
                        data = Base64.decode(value, Base64.DEFAULT);
                    }
                    break;
                case "error":
                    error = node.getValue();
                    break;
            }
        }
        if (unauthorized) {
            return new UnauthorizedDesfireFile(id, error);
        }

        if (error != null) {
            return new InvalidDesfireFile(id, error);
        }
        return DesfireFile.create(id, settings, data);
    }

    @Override public void write(OutputNode node, DesfireFile value) throws Exception {
        throw new SkippableRegistryStrategy.SkipException();
    }
}