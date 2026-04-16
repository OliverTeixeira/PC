import java.nio.channels.InterruptedByTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.*;
import java.util.concurrent.locks.*;

public class WarehouseCooperativa {
    private Map<String, Product> map =  new HashMap<String, Product>();
    private Lock lock = new ReentrantLock(); //acrescentamso o lock de biblioteca q vamos utilizar para o armazem

    private class Product { 
        int quantity = 0; 
        Condition cond = lock.newCondition(); //inicializamos o lock
    }



    private Product get(String item) {
        Product p = map.get(item);
        if (p != null) return p;
        p = new Product();
        map.put(item, p);
        return p;
    }

    public void supply(String item, int quantity) throws InterruptedException{
        lock.lock();
        try{
            Product p = get(item);
            p.quantity += quantity; //mesma coisa do outro
            p.cond.signalAll();
        } finally {
            lock.unlock();
        }
    }

    //aqui, na cooperativa, vamos tentar que a encomenda seja completa antes de retirar produtos do armazem
    public void consume(Set<String> items) throws InterruptedException{
        lock.lock();
        try {
            Product[] a = new Product[items.size()];
            Product p;

            int i = 0;
            for(String s : items){ //criamos uma lista com os pordutos desejados
                a[i++] = get(s);
            }   
            
            while(true){
                p = Unavailable(a); //a funçao auxiliar Unavailable verifica se ha algum produto na lista que nao tenha stock dispponivel, retornando null
                                // caso nao ocorra para nenhum dos produtos. Quando o p é null, quer dizer q todos os elementos da lista estao disponiveis
                                 //logo, podemos sair do ciclo  
                if(p == null){ //caso algum elemento da lista nao esteja disponivel, vamos notificar a Thread, p q ela espere ate esse produto esteja
                    break;     //novamente disponivel e ai voltamos a testar se todos os produtos estao disponiveis 
                } else {
                    p.cond.await();
                }
            }
            for (Product prod : a){ //ja fora do ciclo, vamos retirar 1 de cada produto da lista
                prod.quantity--;
            }
        } finally {
            lock.unlock();
        }
    }

    //metodo auxiliar que retorna o primeiro elemento indisponivel numa lista ou null caso todos estejam disponiveis
    private Product Unavailable (Product[] a){
        for (Product p : a){
            if(p.quantity <= 0){
                return p;
            }
        }
        return null;
    }
    
}
