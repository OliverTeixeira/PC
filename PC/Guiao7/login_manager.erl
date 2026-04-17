-module(login_manager).
-export([start/0, create_account/2, close_account/2, login/2, logout/1, online/0]).

%funçao para iniciar o processo do gestor
start() ->
    Pid = spawn(fun() -> loop([]) end),
    register(?MODULE, Pid).

%funçao auxiliar de RPC (remote procedure call)
rpc(Fun, Args) ->
    ?MODULE ! {self(), {Fun, Args}},
    receive
        {Res, ?MODULE} -> Res
    end.

%API DO CLIENTE
create_account(Username, Password) ->
    rpc({create_account, [Username, Password]}).

close_account(Username, Password) ->
    rpc({close_account, [Username, Password]}).

login(Username, Password) ->
    rpc({login, [Username, Password]}).

logout(Username) ->
    rpc({logout, [Username]}).

online() ->
    rpc({online, []}).

%CICLO DO PROCESSO GESTOR
loop(Map) ->
    receive
        {{create_account, Username, Password}, From} ->
            case maps:find(Username, Map) of
                error ->
                    %cria a conta e define como offline
                    NewMap = maps:put(Username, {Password, false}, Map),
                    From ! {ok, ?MODULE},
                    loop(NewMap);
                _ ->
                    From ! {error, ?MODULE},
                    loop(Map)
            end.

        {{close_account, Username, Password}, From} ->
            case maps:find(Username, Map) of
                %pattern matching para verificar se a conta existe e a senha é correta
                {ok, {Password, _}} ->
                    From ! {ok, ?MODEULE},
                    loop(maps:remove(Username, Map));
                _ ->
                    From ! {error, ?MODULE},
                    loop(Map)
            end;

        {{login, Username, Password}, From} ->
            case maps:find(Username, Map) of
                %existe, a senha ta certa mas ta offline
                {ok, {Password, false}} ->
                    NewMap = maps:put(Username, {Password, true}, Map),
                    From ! {ok, ?MODULE},
                    loop(NewMap);

                %falha se nao exisitr, senha errada ou ja tiver online
                From ! {error, ?MODULE},
                loop(Map)
            end;

        {{logout, Username}, From} ->
            case maps:find(Username, Map) of
                {ok, {Password, true}} ->
                    NewMap = maps:put(Username, {Password, false}, Map),
                    From ! {ok, ?MODULE},
                    loop(NewMap);

                _ ->
                    From ! {error, ?MODULE},
                    loop(Map)
            end;

        {online, From} ->
            %list comprehension para pegar os usuarios online (estado true)
            Users = [User || {User, {_, true}} <- maps:to_list(Map)],
            From ! {Users, ?MODULE},
            loop(Map)
    end.
