-module(locks).
-export([start/0, acquire/1, release/1]).

%API DO CLIENTE
%cria o processo e inicia no estaod free
creat() ->
    spawn(fun() -> free() end).

acquire(Lock, Mode) ->
    %o Mode pode ser read ou write
    Lock ! {acquire, Mode, self()},
    receive
        {ok, Lock} ->
            ok
    end.

release(Lock) ->
    Lock ! {release, self()},
    receive
        {ok, Lock} ->
            ok
    end.


%MAQUINA DE ESTADOS DON GESTOR
%estado1: livre (n tem leitores e nem escritores)
free() ->
    receive
        {acquire, read, From} ->
            From ! {ok, self()},
            reading(1); %passa ao estado de leitura com 1 leitor
        {acquire, write, From} ->
            From ! {ok, self()},
            writing() %passa ao estado de escrita
    end.

%estado 2: leitura (existem n leitores ativos)
reading(Readers) ->
    receive
        {acquire, read, From} ->
            %se chegar outro leitor, deixamos entrar e somamos 1 ao numero de leitores
            From ! {ok, self()},
            reading(Readers + 1);

        {release, From} ->
            %se um leitor sair, subtraimos 1 do numero de leitores
            From ! {ok, self()},
            if Readers =:= 1 -> free(); %se for o ultimo leitor, passamos para o estado livre
               true -> reading(Readers - 1) %se ainda tiver leitores, continuamos no estado de leitura
            end;

        {acquire, write, From} ->
            %EVITAR STARVATION: um escritor quer entrar, entao mudamos o estado p esperar q os leitores atuais saiam
            waiting_writers(Readers, From)
    end.

%estado 3: a espera p escrever (existem n leitores a terminar)
waiting_writer(0, WriterFrom) ->
    %quando os leiotres chegarem a 0, damos ok ao escritor e mudamos o estado
    WriterFrom ! {ok, self()},
    writing();

waiting_writer(Readers, WriterFrom) ->
    receive
        %receive seletivo: novos leitores ou escritores ficam bloqueados na mailbox
        {release, From} ->
            From ! {ok, self()},
            waiting_writer(Readers - 1, WriterFrom)
    end.

%estado 4: escrita (1 escritos ativo)
writing() ->
    receive
        {release, From} ->
            From ! {ok, self()},
            free() %o escritor terminou, voltamso ao inicio
    end.




