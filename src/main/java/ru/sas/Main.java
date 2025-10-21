package ru.sas;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    private static final String propertiesFileName = "C:\\dev\\telecom\\src\\main\\resources\\stdpclient.properties";
//    private static final String propertiesFileName = "..\\src\\main\\resources\\stdpclient.properties";

    public static void main(String[] args)  throws IOException {
        Properties stdpProperties = new Properties();

        System.out.println("stdp client started");
        try ( FileInputStream in = new FileInputStream(propertiesFileName)) {
            stdpProperties.load(in);
        }

        for (String key : stdpProperties.stringPropertyNames()) {
            System.out.println(key + ": " + stdpProperties.getProperty(key));
        }
        System.out.print("---");

        stdpProperties.setProperty("defaultName", "АСОУП");
        // сохраняем настройки из объекта Properties в файл
        try(FileOutputStream out = new FileOutputStream(propertiesFileName)) {
            stdpProperties.store(out, null);
        }

    }
}