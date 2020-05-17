import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

/**
 *
 * @author Prasad
 */
public class Server {

    private static ServerSocket serverSocket = null;
    private static Socket clientSocket = null;

    private static final int maxClientsCount = 100;
    private static final ClientThread[] threads = new ClientThread[maxClientsCount];
    private static int latestWaiting;
    private static boolean isAnyClientWaiting = false;
    static int portNumber = 8000;


    public static void main(String[] args) {


        Enumeration<URL> resources = null;
        try {
            resources = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
        } catch (IOException e) {
            e.printStackTrace();
        }
        int trial = 0;
        while (resources != null && resources.hasMoreElements()) {
            try {
                if (++trial == 3) {
                    break;
                }
                URL manifestUrl = resources.nextElement();

                if (Pattern.compile(".*word-war-server.*jar!/META-INF/MANIFEST.MF").matcher(manifestUrl.getPath()).find()) {
                    System.out.println("Manifest url = " + manifestUrl);
                    Manifest manifest = new Manifest(manifestUrl.openStream());
                    Attributes mainAttributes = manifest.getMainAttributes();
                    String port = mainAttributes.getValue("Port");
                    portNumber = Integer.parseInt(port);
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NumberFormatException e) {
                e.printStackTrace();
                portNumber = 8000;
            }
        }

        System.out.println("Using port number = " + portNumber);

        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                clientSocket = serverSocket.accept();
                int i;

                for (i = 0; i < maxClientsCount; i++) {
                    if (threads[i] == null) {
                        (threads[i] = new ClientThread(clientSocket, threads, i, maxClientsCount)).start();
                        System.out.println(i + ": client thread created");
                        break;
                    }
                }

                if (i == maxClientsCount) {
                    System.out.println("Server busy");
                    PrintWriter outputStreamPrinter = new PrintWriter(clientSocket.getOutputStream(), true);
                    outputStreamPrinter.println("Server too busy. Try later.");
                    outputStreamPrinter.close();
                    clientSocket.close();
                    System.out.println(i + ": client connection closed");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static synchronized void findOpponent(int index) {
        if (isAnyClientWaiting) {
            threads[latestWaiting].oppIndex = index;
            threads[latestWaiting].latch.countDown();
            threads[index].oppIndex = latestWaiting;
            threads[index].randomString = threads[latestWaiting].randomString;
            System.out.println(index + "-" + threads[index].name + ": is matched with " + latestWaiting + "-" + threads[latestWaiting].name);
        } else {
            latestWaiting = index;
            threads[index].randomString = getRandomString();
            System.out.println(index + "-" + threads[index].name + ": waiting for opponent");
        }
        isAnyClientWaiting = !isAnyClientWaiting;
    }

    private static final double[] cumulativeFrequency = new double[26];
    private static final double[] frequency = {0.08167, 0.01492, 0.02782, 0.04253, 0.12702, 0.02228, 0.02015, 0.06094, 0.06966, 0.00153, 0.00772, 0.04025, 0.02406, 0.06749, 0.07507, 0.01929, 0.00095, 0.05987, 0.06327, 0.09056, 0.02758, 0.00978, 0.02360, 0.00150, 0.01974, 0.00075};

    static String getRandomString() {

        double sum = 0;
        for (int i = 0; i < 26; i++) {
            sum += frequency[i];
            cumulativeFrequency[i] = sum;
        }

        StringBuilder randomString = new StringBuilder();
        int[] count = new int[26];
        for (int i = 0; i < 16; i++) {

            double ran = Math.random();
            int j = 0;
            while (cumulativeFrequency[j] < ran) {
                j++;
            }
            count[j]++;
            if (count[j] >= 3) {
                i--;
            } else {
                randomString.append((char) (j + 65));
            }
        }
        return randomString.toString();
    }
}

class ClientThread extends Thread {

    static int numberOfPlayersOnline = 0;
    private static int maxClientsCount;

    private BufferedReader inputStreamReader = null;
    private PrintWriter outputStreamPrinter = null;
    private Socket clientSocket;
    private final ClientThread[] threads;
    CountDownLatch latch = new CountDownLatch(1);
    private CountDownLatch rematchLatch = new CountDownLatch(1);
    String randomString;
    String name;
    boolean rematch;
    int index;
    volatile int oppIndex = -1;

    ClientThread(Socket clientSocket, ClientThread[] threads, int index, int maxClientsCount) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        this.index = index;
        ClientThread.maxClientsCount = maxClientsCount;
    }

    public void run() {
        ClientThread[] threads = this.threads;
        try {
            inputStreamReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outputStreamPrinter = new PrintWriter(clientSocket.getOutputStream(), true);
            outputStreamPrinter.println("" + numberOfPlayersOnline++);
            System.out.println("Number of players online = " + numberOfPlayersOnline);

            name = inputStreamReader.readLine().trim();
            if (name.equals("_abort_")) {
                System.out.println(index + "-" + threads[index].name + ": aborted");
                closeThread();
                return;
            }
            System.out.println(index + ": name  = " + name);

            String s;

            while (!(s = inputStreamReader.readLine()).equals("left_game")) {
                System.out.println(index + "-" + threads[index].name + ": inputMessage - " + s);
                if (s.equals("_start_") || s.equals("new_match")) {

                    if (s.equals("new_match") && oppIndex != -1) {
                        threads[oppIndex].rematchLatch.countDown();

                        threads[oppIndex].outputStreamPrinter.println("left_game");
                        oppIndex = -1;
                        System.out.println(index + "-" + threads[index] + ": initiated new match but still has opponent");
                    }

                    Server.findOpponent(index);
                    if (oppIndex == -1) {
                        latch.await();
                    }

                    latch = new CountDownLatch(1);

                    if (s.equals("new_match")) {
                        outputStreamPrinter.println("new_match");
                    }
                    outputStreamPrinter.println(threads[oppIndex].name);
                    outputStreamPrinter.println(randomString);
                    System.out.println(index + "-" + threads[index] + ": oppName and randomString sent");
                    continue;
                }

                if (s.equals("_rematch_")) {
                    rematch = true;
                    if (threads[oppIndex] == null) {
                        oppIndex = -1;
                        rematch = false;
                        continue;
                    }

                    threads[oppIndex].outputStreamPrinter.println("_rematch_");

                    if (index < oppIndex) {
                        randomString = Server.getRandomString();
                        threads[oppIndex].randomString = randomString;
                        System.out.println(index + "-" + threads[index] + ": set randomString = " + randomString);
                    }

                    threads[oppIndex].rematchLatch.countDown();
                    System.out.println(index + "-" + threads[index] + ": waiting for rematch");
                    rematchLatch.await();
                    rematchLatch = new CountDownLatch(1);

                    if (oppIndex != -1) {
                        System.out.println(index + "-" + threads[index] + ": rematch accepted");
                        outputStreamPrinter.println(randomString);
                    }

                    rematch = false;
                    continue;
                }

                if (s.equals("opp_left")) {
                    oppIndex = -1;
                    continue;
                }
                threads[oppIndex].outputStreamPrinter.println(s);
            }

            if (oppIndex != -1) {
                threads[oppIndex].rematchLatch.countDown();
                threads[oppIndex].rematchLatch = new CountDownLatch(1);
                threads[oppIndex].outputStreamPrinter.println(s);
                System.out.println(index + "-" + threads[index] + ": sent left_game to opponent");
            }
            closeThread();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void closeThread() throws IOException {
        for (int i = 0; i < maxClientsCount; i++) {
            if (threads[i] == this) {
                threads[i] = null;
                break;
            }
        }
        inputStreamReader.close();
        outputStreamPrinter.close();
        clientSocket.close();
        numberOfPlayersOnline--;
        System.out.println(index + "-" + name + ": closed thread");
    }
}
