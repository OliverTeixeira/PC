public class Guiao4EX1 {

    class Barrier {
        int N = 0;
        private int counterA = 0;
        private int counterRE = 0;
        private int counterRE2 = 0;

        Barrier (int N) {
            this.N = N;
        }

        //versao em que: 1. Suponha que cada thread apenas vai invocar await uma vez sobre o objecto
        public synchronized void await() throws InterruptedException {
            //vamos contar a threads que entram
            counterA++;

            //enquanto nao for o num de threads que a gente quer, fazemos com que as threads esperem
            while(counterA < N){
                wait();
            }

            //quando chegar todas as threads que a gente quer, ela (a uktima thread) vai notificar as outras threads q tavam bloqueadas
            if (counterA == N){
                notifyAll();
            }
        }
        private boolean flagOpen = true;

        //versao da segunda linha, em que deve permitir que a Barreira seja reutilizavel
        public synchronized void reWait() throws InterruptedException {
            //nesse caso, ao inves de contar todas as theads, vamos utilizar uma flag para nos dizer que o num de threads estao ou nao preenchidas
            //caso estejam preenchidas, as novas threads vao yer de aguardar aqui ate q seja possivel entrar na barreira
            while(!flagOpen){
                wait();
            //TEM QUE SER UM WHILE E NAO UM IF, POIS SE FOR UM IF, pode fazer com que acorde as threads espontaneamente, ou seja, sem ter sido notificada!!!
            }
            //contamos as threads que vao entrando
            counterRE++;

            //se a barreira estiver aberta e ainda nao tiver chegado ao num de threads q precisamos, as que chegarem vao entrando nesse while
            while(counterRE < N && flagOpen){
                wait();
            } 

            //quando chegar a ultima thread e se a barreira estiver aberta, vamos fcehar a barreira e ativar todas as threads
            if(counterRE == N && flagOpen){
                flagOpen = false;
                notifyAll();
            }
            //decretamos o contador de threads
            counterRE--;

            //quando o contador estiver a , damos reset e abrimos a barreira e ecordamos as threads que estao bloqueadas
            if(counterRE == 0 && !flagOpen){
                flagOpen = true;
                notify();
            }

            //ESSA VERSAO TEM UM ERRO QUE É: quanso mais de N threads que estavam a espera a entrada sao ativadas ao mesmo tempo, de modo com que algumas 
            //threads passaram pela seccao sem fazer nada.
        }

        private int grupoNr = 0;
        //VERSAO SO PROFFESSOR
        public synchronized void reWait2() throws InterruptedException {
            //vamos asocias a cada N elementos a um grupo espeficifico, avançando o grupo atual sempre q N threads se juntem
            final int grupoNr = this.grupoNr;

            counterRE2++;

            if(counterRE2 == N){
                counterRE2 = 0; //quando atingirmos as N threads necessarias, damos reset ao contador
                this.grupoNr++; //avançamos o grupo atual
                notifyAll(); //acordamso as threads que estao bloqueadas
            } else {
                while(grupoNr == this.grupoNr){ //se nao tiver preenchido o grupo, esperamso ate que preencha
                    wait();
                }
            }
        }



    }
}