import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.*;
import java.util.concurrent.locks.*;


public class WarehouseEgo {
    private Map<String, Product> map =  new HashMap<String, Product>();
    private Lock lock = new ReentrantLock();//acrescentamos o lock de biblioteca q vamos utilizar no armazem



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

    public void supply(String item, int quantity) throws InterruptedException { //nesta funçao, temos apenas de aplicatr o lock de biblioteca
        lock.lock();
        try{
            Product p = get(item);
            p.quantity += quantity;
            p.cond.signalAll(); //diferente dos guioes anteriores, nesse a gente vai utilizar as condiçoes, vamos reativar as threads q estivessem bloqueadas com 
                                //a seguinte condiçao: ha x produto disponivel
        } finally {
            lock.unlock();
        }
    }

    //essa versao é egoista pq nela, foca em cada cliente de forma igual. Mal um produto estja disponivel, um cliente q estivesse a espera pode logo recolhe-lo mesmo 
    //que deixe o resto da encomneda incompleta
    public void consume(Set<String> items) throws InterruptedException {
        lock.lock();
        try{
            for (String s : items) {
                Product product = get(s);      //neste caso vamos associar à Thread a condiçao que é: nao haver o produto. Assim, mesmo com o lock
                while(product.quantity <= 0){  //conseguimos mais flexibilidade sobre qauis Threads ativar em determinados momentos.
                    product.cond.await();
                }
                product.quantity--;
            }
        } finally {
            lock.unlock();
        }
    }
    
}
