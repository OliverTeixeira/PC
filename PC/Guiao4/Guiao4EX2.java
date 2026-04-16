public class Guiao4EX2 {
    class Agreement {
        private int N = 0;
        private int counter = 0;
        private int guessNr = 0;
        private int max = 0;
        private int maxAux = 0;

        Agreement (int N) {
            this.N =N;
        }

        int propose (int choice) throws InterruptedException {
            //primeiro fixamos na Thread o numero de tentativa na qual estamos 
            final int guessNr = this.guessNr;

            //guardamos como max o maior numero entre o maximo atual e a nova guess
            max = Math.max(choice, max);
            counter++; //contamos mais uma thread q entrou

            //quando atingirmos N threads vamos:
            if(counter == N){
                maxAux = max; //guardar o maximo atual numa variavel auxiliar
                max = 0; //resetar o maximo atual para que outras threads nao sejam influenciadas
                counter = 0; //resetar o contador para comecar a contas as threads do inicio na nova ronda
                this.guessNr++; //avançar o numero de tentativa
                notifyAll(); //acordamos as threads que estao bloqueadas
            } else {
                while(guessNr == this.guessNr) {
                    wait(); //se nao tiver atingido o numero de threads necessario, as threads vao ficar bloqueadas aqui
                }
            }
            return maxAux;
        }
    }  
}
