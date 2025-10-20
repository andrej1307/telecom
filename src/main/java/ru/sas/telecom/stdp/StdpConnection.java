package ru.sas.telecom.stdp;

import ru.sas.telecom.exception.StdpException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Properties;

public class StdpConnection extends Thread {
    private static final int STDP_ID_ABON_LENGTH = 13;

    protected Properties connectionProperties;
    protected Socket sock;
    protected DataOutputStream output;	// поток для передачи информации
    protected DataInputStream input;    // поток для приема информации

    protected boolean inAction = false;	// признак выполнения цикла обмена данными
    protected boolean isConnected = false;

    String serverResponseText;
    String lastErrorText;

    public StdpConnection(Properties properties) {
        super();
        this.connectionProperties = properties;
    }

    public void run() throws StdpException {
        // Проверяем корректность собственного идентификатора клиента - длина 13 символов
        if (connectionProperties.getProperty("localID", "").length() != STDP_ID_ABON_LENGTH) {
            throw new StdpException("Собственный идентификатор клиента STDP задан не верно.");
        }
        if (connectionProperties.getProperty("serverID", "").length() != STDP_ID_ABON_LENGTH) {
            throw new StdpException("Идентификатор сервера STDP задан не верно.");
        }
        if (connectionProperties.getProperty("serverIp", "").isEmpty()) {
            throw new StdpException("IP адрес сервера STDP не задан.");
        }

        try {
            // создаем сокет соединения
            sock = new Socket(connectionProperties.getProperty("serverIP"),
                    Integer.decode(connectionProperties.getProperty("serverPort", "8085")));
            // открываем потоки ввода вывода связанные с сокетом TCP соединения
            output = new DataOutputStream(sock.getOutputStream());
            input = new DataInputStream(sock.getInputStream());

            // формируем текст запроса на соединение с сервером STDP
            String outputUnit = "STDP0480000000003"
                    + connectionProperties.getProperty("localID")
                    + connectionProperties.getProperty("serverID")
                    + "10000";

            // Преобразуем текст в поток байтов ДКОИ и передаем запрос в канал
            byte [] bytesForOutput = outputUnit.getBytes("1025");
            output.write(bytesForOutput, 0, 48);
            inAction = true;
        }
        catch (Exception e) {
            throw new StdpException("Ошибка соединения с сервером " + connectionProperties.getProperty("serverIP")
                    + ":" + connectionProperties.getProperty("serverPort", "8085")
                    + "\n Системное исключение: " + e.getMessage() );
        }

        StringBuilder inputUnit = new StringBuilder();
        while (inAction) {
            inputUnit.delete(0,inputUnit.length());
            try {
                // проверяем число байт доступных для чтения из канала
                int bytesForRead = input.available();

                // если в канале есть данные, то читаем начальные 17 байт - обязательные поля
                if (bytesForRead > 0) {
                    byte [] inputBytes = input.readNBytes(17); 			        // читаем заголовок
                    inputUnit.append(new String(inputBytes, "1025"));   // сохраняем начало пакета
                }

                // читаем общую длину заголовка с 5 по 7 символ (длина вместе с переменной частью)
                int headLength = Integer.parseInt(inputUnit.substring(4, 7));
                bytesForRead = headLength - 17;

                if (bytesForRead > 0) {
                    byte [] inputBytes = input.readNBytes(bytesForRead);	        // читаем остаток заголовка
                    inputUnit.append(new String(inputBytes, "1025"));   // пополняем паринятый пакет
                }

                // читаем тип информационной единицы
                String unitType = inputUnit.substring(15, 17);

                switch(unitType) {
                    case "02": // принят ответ на запрос IP
                        break;
                    case "04": // принят ответ на запрос установления соединения
                        stdpConnectResponse(inputUnit.toString());
                        break;
                    case "05": // принято сообщение
                        break;
                    case "06":
                        break;
                    case "07": // принят "разрыв соединения"
                        break;
                    case "08": // принят пакет "ТЕСТ"
                        stdpTest();
                        break;
                    case "09":
                        break;
                    case "10":
                        break;
                    default:
                }

            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void stdpConnectResponse(String inputUnit) {
        String responseCode = inputUnit.substring(48, 50);
        switch (responseCode) {
            case "00":
                serverResponseText = connectionProperties.getProperty("localID")
                        + " установлено соединение с сервером "
                        + connectionProperties.getProperty("serverIP");
                isConnected = true;
                break;
            case "01":
                serverResponseText = connectionProperties.getProperty("localID")
                        + " некорректно оформлен заголовок.";
                isConnected = false;
                break;
            case "02":
                serverResponseText = connectionProperties.getProperty("localID")
                        + " автоответ клиента не определен.";
                isConnected = false;
                break;
            case "03":
                serverResponseText = connectionProperties.getProperty("localID")
                        + " автоответ сервера не определен.";
                isConnected = false;
                break;
            case "04":
                serverResponseText = connectionProperties.getProperty("localID")
                        + " повторное вхождение в связь.";
                isConnected = true;
                break;
            case "05":
                serverResponseText = connectionProperties.getProperty("localID")
                        + " логический канал связи СТД закрыт.";
                isConnected = false;
                break;
            case "06":
                serverResponseText = connectionProperties.getProperty("localID")
                        + " попытка несанкционированного доступа.";
                isConnected = false;
                break;
        }

    }

    private void stdpTest() {
        // формируем текст ответа на "ТЕСТ"
        String tmp = "STDP0470000000009"
                + connectionProperties.getProperty("localID")
                + connectionProperties.getProperty("serverID")
                + "0000";

        try {
            // Формируем массив байт в кодировке ДКОИ
            byte[] bytes = tmp.getBytes("1025");
            sendBytes(bytes, 47);
        } catch (Exception e) {
            lastErrorText = e.toString();
        }
    }

    /**
         * синхронизованный метод передачи данных в сокет
         * @param bytes - массив байт для передачи
         * @param num - число байт для передачи
         */
    synchronized private void sendBytes(byte [] bytes, int num) {
        try {
            output.write(bytes, 0, num);
        } catch (Exception e) {
            lastErrorText = " Ошибка при передаче данных. Системное исключение.\n"
                    + e.getMessage();
        }
    }

}
