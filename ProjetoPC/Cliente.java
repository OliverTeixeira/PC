import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        try{
            //liga a porta 8080 no proprio pc
            Socket socket = new Socket("localhost", 8080);
            //ferramentas para ler e p escrever
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //scanner p ler oq escrevemos no teclado
            Scanner scanner = new Scanner(System.in);

            System.out.println("Ligado ao servidor! Escreve comandos (ex: FRENTE, ESQUERDA). Escreve 'sair' p fechar");

            //ciclo continuo
            while(true){
                String comando = scanner.nextLine(); //le do meu teclado

                if(comando.equalsIgnoreCase("sair")){
                    break;
                }

                out.println(comando); //envia p o erlang

                String resposta = in.readLine(); //le a resposta do erlang
                System.out.println("Resposta: " + resposta);
            }

            //limpeza quando fechas
            socket.close();
            scanner.close();
            System.out.println("Desligado.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
