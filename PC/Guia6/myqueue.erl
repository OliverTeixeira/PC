-module(myqueue). %define o nome do ficheiro e do modulo
-export([create/0, enqueue/2, dequeue/1, test/0]). % diz ao erlang quais as funçoes que podem ser chamadas fora desse ficheiro. o num da frente da / diz a aridade
%e indica quantos argumentos a funçao recebe

%funçao que cria apenas uma queue vazia
create() -> [].

%funçao "enqueue"  que vai inserir um novo elemento numa queue ja existente 
enqueue([], Item) -> [Item];
enqueue([H | T], Item) -> [H | enqueue(T, Item)].

%funçao "dequeue que vai remover um elemento de uma queue ja existente"
dequeue([]) -> empty;
dequeue([H | T], Item) -> {T, H}. %devolve um Tuplo, onde é a nova fila sem o primeiro elemento, e o elemento que acabou de ser removido

%funçao de teste so p verificar se ta certo
test() -> 
    Q0 = create(),
    Q1 = enqueue(Q0, 1),
    Q2 = enqueue(Q1, 2),
    Q3 = enqueue(Q2, 3),
    Q4 = enqueue(Q3, 4),
    Q5 = enqueue(Q4, 5),

    io:format("Queue apos 5 inserçoes: ~p~n", [Q5]),

    {Q7, V1} = dequeue(Q5),
    {Q8, V2} = dequeue(Q7),
    {Q9, V3} = dequeue(Q8),
    {Q10, V4} = dequeue(Q9),
    {Q11, V5} = dequeue(Q10),

    io:format("Valores removidos: ~p, ~p, ~p, ~p, ~p~n", [V1, V2, V3, V4, V5]),
    io:format("Queue final: ~p~n", [Q11]),

    empty = dequeue(Q11),
    ok.
