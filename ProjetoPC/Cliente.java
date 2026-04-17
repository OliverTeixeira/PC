import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int porta = 8080;

        try (Socket socket = new Socket(host, porta);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner teclado = new Scanner(System.in)) {

            System.out.println("--- Conectado ao Servidor Erlang ---");
            System.out.println("Podes comecar a escrever (ex: register#p1#123)");

            // A MAGIA: Uma Thread dedicada só para ouvir o Erlang em tempo real!
            Thread ouvinte = new Thread(() -> {
                try {
                    String mensagemDoServidor;
                    while ((mensagemDoServidor = in.readLine()) != null) {
                        System.out.println(">> " + mensagemDoServidor);
                    }
                } catch (IOException e) {
                    System.out.println("Conexao com o servidor perdida.");
                }
            });
            ouvinte.start();

            // O ciclo principal fica apenas focado em ler o teu teclado
            while (true) {
                String comando = teclado.nextLine();
                out.println(comando); 
            }

        } catch (IOException e) {
            System.out.println("Erro ao conectar: Verifica se o Erlang esta a correr!");
        }
    }
}