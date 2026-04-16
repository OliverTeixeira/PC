-module(server). %da nome ao ficheiro
%diz quais funçoes podem ser chamadas de fora deste ficheiro
-export([start/1]).


%1. Arranque do servidor
start(Port) ->
    spawn(fun() -> init_server(Port) end).

%o nosso loop vai receber o socket e as coordenadas iniciais
init_server(Port) ->
    {ok, LSock} = gen_tcp:listen(Port, [binary, {packet, line}, {reuseaddr, true}, {active, true}]),
    io:format("Servidor a escutar na porta ~p~n", [Port]),

    %Craimos o processo do lobby. Ele começa com uma fila vazia [] ,temporizador e mapa de jogos ativos #{}
    LobbyPid = spawn(fun() -> lobby_loop([], undefined, #{}) end),

    %loop de utilizadores. Agr vamos passar o LobbyPid p o main_loop saber p onde mandar os jogadores
    UsersPid = spawn(fun() -> main_loop(#{}, LobbyPid) end),

    %arrancamos o nosso porteiro
    spawn(fun() -> acceptor(LSock, UsersPid) end),

    %mantem o processo inicial vivo
    wait_loop().

wait_loop() ->
    receive
        stop -> ok;
        _ -> wait_loop()
    end.

%2. Aceitar conexões e passar o testemunho (o porteiro)
acceptor(LSock, UsersPid) -> 
    {ok, Sock} = gen_tcp:accept(LSock), 
    io:format("Novo jogador conectado. Socket: ~p~n", [Sock]),

    %faz spawn de um novo porteiro p ficar a espera do proximo cliente
    spawn(fun() -> acceptor(LSock, UsersPid) end),

    %inicia o tratamento do cliente
    client(Sock, UsersPid).

%3.O assitente do Cliente (le o tcp e mnada p o main_loop)
client(Sock, UsersPid) ->
    receive
        %como temos {active, true}, os dados chegam como mensagens erlang
        {tcp, Sock, Data} ->
            %converte de binario p lista(string) e tira o \n do fim
            String = string:trim(binary_to_list(Data)),
            io:format("Client_process (~p): Recebeu texto '~s'~n", [self(), String]),

            %1. traduz a string p uma mensgame estruturada erlang
            MensagemEstruturada = parse_input(String, Sock),

            %2.manda a mensgame ja traduzida p o loop principal
            UsersPid ! MensagemEstruturada,

            %3. fica a espera do proximo comando
            client(Sock, UsersPid);

        {tcp_closed, Sock} -> 
            io:format("Client_process (~p): Jogador desconectou-se.~n", [self()]),
            ok;
        {error, Sock, Reason} ->
            io:format("Client_process (~p): Erro ~p: ~n", [self(), Reason]),
            ok
    end.


%função de parsing
parse_input(String, Sock) ->
    %cortamos a string utilizando # como separador. ex: login#ze#123 vira ["login", "ze", "123"]
    case string:lexemes(String, "#") of
        ["register", UserName, Password] ->
            {register, UserName, Password, Sock};

        ["login", Username, Password] ->
            {login, Username, Password, Sock};
            
        ["join_lobby", Username] ->
            {join_lobby, Username, Sock};

        ["move", Username, Tecla] ->
            {movimento, Username, Tecla, Sock};
        _ -> 
            {comando_desconhecido, String, Sock} %isso se o java mandar algo q n conhecemos
    end.


%4. O cerebro central (onde fica os dados)
main_loop(UsersMap, LobbyPid) ->
    receive
        {register, UserName, Password, Sock} ->
            %vamos verificar se o username ja esta nas chaves do nosso mapa
            case maps:is_key(UserName, UsersMap) of
                true ->
                    gen_tcp:send(Sock, <<"ERRO! Utilizador já existe\n">>),
                    main_loop(UsersMap, LobbyPid);
                false ->
                    %se n existe, criamos a conta {Password, Nivel=1, Vitorias=0, Derrotas=0}
                    DadosConta = {Password, 1, 0, 0},
                    NovoMapa = maps:put(UserName, DadosConta, UsersMap),

                    gen_tcp:send(Sock, <<"Registo bem-sucedido\n">>),
                    io:format("Novo registo para: ~p~n", [UserName]),
                    main_loop(NovoMapa, LobbyPid)
            end;

        {login, Username, Password, Sock} ->
            %procura a conta no mapa
            case maps:find(Username, UsersMap) of
                %se a conta existir, o erlang extrai os dados
                {ok, {PassGuardada, _Nivel, _Vitorias, _Derrotas}} when PassGuardada == Password -> %o when verifica a password
                    gen_tcp:send(Sock, <<"Login bem-sucedido\n">>),
                    io:format("Login bem-sucedido para: ~p~n", [Username]),
                    main_loop(UsersMap, LobbyPid);

                %se a conta existir mas a password n bate com o when
                {ok, _DadosDiferentes} ->
                    gen_tcp:send(Sock, <<"Erro: Password incorreta\n">>),
                    main_loop(UsersMap, LobbyPid);

                %se a conta n existir
                error ->
                    gen_tcp:send(Sock, <<"Erro: Utilizador não encontrado\n">>),
                    main_loop(UsersMap, LobbyPid)
            end;

        {join_lobby, Username, Sock} ->
            io:format("Main_Loop: ~p quer entrar na fila p jogar~n", [Username]),
            %enviamos a mensagem p o lobby
            LobbyPid ! {entrar_fila, Username, Sock},
            main_loop(UsersMap, LobbyPid);

        {movimento, UserName, Tecla, _Sock} ->
            LobbyPid ! {movimento, UserName, Tecla},
            main_loop(UsersMap, LobbyPid);

        {comando_desconhecido, String, Sock} ->
            io:format("Main_Loop: Comando desconhecido: ~p~n", [String]),
            gen_tcp:send(Sock, <<"Erro: Comando não reconhecido\n">>),
            main_loop(UsersMap, LobbyPid)
        end.

%esse processo vai guardar a lista de espera. Sempre que chega a 3 pessoas, ele "corta" as 3 primeiras e avisa que vai começar
lobby_loop(Queue, TimerRef, ActiveGamesMap) ->
    receive
        {entrar_fila, Username, Sock} ->
            NovaQueue = Queue ++ [{Username, Sock}],
            NumJogadores = length(NovaQueue),
            io:format("Lobby: ~p entrouna fila. Total na fila: ~p~n", [Username, NumJogadores]),
            gen_tcp:send(Sock, <<"Entrou na fila de espera\n">>),

            if
                %Cenario A: temos 4 jogadores entao arrancamos logo com o Maximo
                NumJogadores == 4 ->
                    io:format("Lobby: Fila cheia(4 jogadores). Iniciando jogo...~n"),

                    %se havia crononometro a contar, cancelamos
                    if TimerRef =/= undefined -> erlang:cancel_timer(TimerRef); true -> ok end,

                    %tira os 4 primeiros da fila 
                    {JogadoresPartida, RestoFila} = lists:split(4, NovaQueue),



                    %criamos a sala e guardamos o PID dela
                    GamePid = spawn(fun() -> init_game(JogadoresPartida) end),
                    NovoMapaJogos = registar_jogadores(JogadoresPartida, GamePid, ActiveGamesMap),

                    %volta ao inicio com o resto da fila sem o timer
                    lobby_loop(RestoFila, undefined, NovoMapaJogos);

                %Cenario B: temos o minimo de 3 jogadores e entao iniciamos a contgem p o quarto
                NumJogadores == 3 ->
                    io:format("Lobby: 3 jogadores na fila. Iniciando a contagem de 10s para o jogador 4...~n"),

                    %o erlang envia a mensagem "tempo_esgotado" p si mesmo daqui a 10s
                    NovoTimer = erlang:send_after(10000, self(), tempo_esgotado),

                    %volta ao inicio a guardar a ref do NovoTimer
                    lobby_loop(NovaQueue, NovoTimer, ActiveGamesMap);

                %Cenario C: temos 1 ou 2 jogadores, entao so eperamos
                true ->
                    lobby_loop(NovaQueue, TimerRef, ActiveGamesMap)
            end;

        %Cenario D: passaram os 10s e o Erlang recebeu a mensagme do passado
        tempo_esgotado ->
            NumJogadores = length(Queue),
            if
                NumJogadores >= 3 ->
                    io:format("Lobby: Tempo esgotado. Inicinaod jogo com 3  jogadores...~n"),
                    {JogadoresPartida, RestoFila} = lists:split(3, Queue),


                    %igual p os 3 jogadores
                    GamePid = spawn(fun() -> init_game(JogadoresPartida) end),
                    NovoMapaJogos = registar_jogadores(JogadoresPartida, GamePid, ActiveGamesMap),
                
                    %volta ao inicio com o resto da fila sem o timer
                    lobby_loop(RestoFila, undefined, NovoMapaJogos);

                true ->
                    %prevençao p caso alguem tiver desconectado e ja nao houvesse 3
                    lobby_loop(Queue, undefined, ActiveGamesMap)
            end;

        %sistema nervoso funcionando    
        {movimento, Username, Tecla} ->
            case maps:find(Username, ActiveGamesMap) of
                {ok, GamePid} ->
                    %encontrou a sala do jogador
                    GamePid ! {movimento, Username, Tecla};
                error ->
                    io:format("Lobby: Recebeu comando de movimento de ~p, mas ele não está em nenhum jogo ativo.~n", [Username])
            end,
            lobby_loop(Queue, TimerRef, ActiveGamesMap)
    end.    

%SALA DE JOGO (motor de fisica e estado do jogo)
init_game(Jogadores) ->
    io:format("GameRoom (~p): A inicializar nova partida com ~p jogadores...~n", [self(), length(Jogadores)]),

    %vamos dar as coordenadas iniciais a cada jogador. P ja vamos separa-los por 100 pixels no X
    %A estrutura sera: #{ "p1" => {X, Y, Raio/Massa, Socket}}
    EstadoInicial = setup_jogadores(Jogadores, #{}, 100),

    %avisa os jogadores q a partida começou e manda a sua posiçao inicial
    enviar_estado_inicial(maps:to_list(EstadoInicial)),

    %o motor arranca. Manda uma mensagem "tick" a cada 2s (so por enuqnaot pq isso é mt sendo o ideal uns 30ms)
    erlang:send_after(2000, self(), tick),

    %arranca o loop do jogo
    game_loop(EstadoInicial).


%Funçao auxiliar q coloca cada jogador no mapa do jogo
setup_jogadores([], MapaState, _) -> MapaState;
setup_jogadores([{Username, Sock} | Resto], MapaState, PosicaoX) ->
    %os jogadores começam com y = 250 e raio = 20
    DadosJogador = {PosicaoX, 250, 20, Sock},
    NovoMapaState = maps:put(Username, DadosJogador, MapaState),

    %continua p o proprio jogador, empurrando-o 100 pixels p o lado
    setup_jogadores(Resto, NovoMapaState, PosicaoX + 100).

%Funçao auxiliar p mandar as boas vindas do jogo
enviar_estado_inicial([]) -> ok;
enviar_estado_inicial([{Username, {X, Y, Raio, Sock}} | Resto]) ->
    %o lists:flatten transforma as listas completas em uma string normal.
    Mensagem = lists:flatten(io_lib:format("O JOGO COMECOU! Tu es o ~s. Posicao Inicial: X=~p, Y=~p, Massa=~p~n", [Username, X, Y, Raio])),
    gen_tcp:send(Sock, list_to_binary(Mensagem)),
    enviar_estado_inicial(Resto).

%ESSE É O LOOP Q VAI FICAR ETERNAMENTE A CALCULAR AS COLISOES E MOVIMENTOS
game_loop(GameState) ->
    receive
        tick ->
            io:format("GameRoom (~p): Tick! Calculando movimentos e colisões...~n", [self()]),

            %aqui mais p frente vamos recalcular as posiçoes dos jogadores e enviar p os clientes java

            %o coraçao tem q continuar batendo: agendamos o proximo "tick"
            erlang:send_after(2000, self(), tick),

            %recomenda o loop com o estado atualizado.
            game_loop(GameState);

        {movimento, Username, Tecla} ->
            io:format("GameRoom: Recebeu comando de movimento de ~p: ~p!~n", [ Username, Tecla]),

            %aqui no futuro vamos calcular o angul e a velocidade do GameState

            game_loop(GameState);

        _ -> game_loop(GameState)
    end.


%funçao auxliar q guarda o GamePid de cada jogador no ActiveGamesMap
registar_jogadores([], _GamePid, Mapa) -> Mapa;
registar_jogadores([{Username, _Sock} | Resto], GamePid, Mapa) ->
    NovoMapa = maps:put(Username, GamePid, Mapa),
    registar_jogadores(Resto, GamePid, NovoMapa).
