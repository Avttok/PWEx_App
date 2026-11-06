package com.pwexpress.feature.gestor.data

/** Catálogo das configurações (gerado do site: src/features/configuracoes/definicoes.ts). */
data class Grupo(val chave: String, val titulo: String, val descricao: String)

data class Param(
    val chave: String, val grupo: String, val rotulo: String, val descricao: String, val descricaoDesligado: String?,
    val tipo: String, val padraoBool: Boolean, val padraoNum: Double, val unidade: String?,
    val min: Double?, val max: Double?, val passo: Double, val opcoes: List<Pair<Double, String>>,
)

val GRUPOS_CONFIG = listOf(
    Grupo("despacho", "Despacho", "Como as corridas são oferecidas aos motoboys."),
    Grupo("planejadas", "Corridas planejadas", "Corridas com hora marcada que os motoboys reservam pelo app."),
    Grupo("local", "Etapas por localização", "Exigir que o motoboy esteja no endereço para avançar."),
    Grupo("preco", "Preço", "Cálculo do valor da corrida (clientes podem ter regra própria)."),
    Grupo("distancia", "Acréscimo por distância", "Corridas longas ficam mais caras, por faixas de distância."),
    Grupo("comprovantes", "Comprovantes padrão", "O que já vem marcado ao lançar uma corrida."),
    Grupo("rastreamento", "Rastreamento no app", "Ritmo do GPS. O app aplica ao sincronizar."),
    Grupo("teste", "Modo teste", "Ferramentas para contas gestor + motoboy em corridas de teste."),
)

val PARAMETROS = listOf(
    Param("oferta_segundos", "despacho", "Tempo para aceitar", "Quanto tempo cada motoboy tem para aceitar a corrida antes dela passar ao próximo.", null, "numero", false, 30.0, "s", 10.0, 180.0, 1.0, emptyList()),
    Param("raio_motoboy_max_km", "despacho", "Raio máximo do motoboy", "Até quanto cada motoboy pode escolher no raio de disponibilidade dele. Corridas urgentes ignoram o raio.", null, "numero", false, 100.0, "km", 1.0, 500.0, 5.0, emptyList()),
    Param("offline_auto_min", "despacho", "Offline automático", "Disponível sem enviar posição há esse tempo vira offline sozinho; volta quando a posição chegar. 0 = desligado.", null, "numero", false, 30.0, "min", 0.0, 240.0, 5.0, emptyList()),
    Param("posicao_max_min", "despacho", "Posição recente", "Só entra na cascata quem enviou posição dentro desse tempo.", null, "numero", false, 15.0, "min", 1.0, 120.0, 1.0, emptyList()),
    Param("raio_max_despacho_km", "despacho", "Distância máxima até a coleta", "Motoboys mais longe que isso não recebem a corrida. 0 = sem limite.", null, "numero", false, 0.0, "km", 0.0, 200.0, 1.0, emptyList()),
    Param("favoritos_primeiro", "despacho", "Favoritos do cliente primeiro", "Na cascata, oferecer antes aos motoboys favoritos do cliente.", null, "booleano", true, 0.0, null, null, null, 1.0, emptyList()),
    Param("agendada_antecedencia_min", "planejadas", "Antecedência mínima", "Só dá para planejar corridas que começam pelo menos isso depois de agora.", null, "numero", false, 30.0, "min", 5.0, 1440.0, 5.0, emptyList()),
    Param("agendada_cancelar_ate_min", "planejadas", "Desistência até", "Quem reservou pode desistir pelo app até esse tempo antes do horário. Depois, só o gestor libera.", null, "numero", false, 60.0, "min", 0.0, 2880.0, 15.0, emptyList()),
    Param("agendada_iniciar_antes_min", "planejadas", "Pode iniciar a partir de", "Quanto tempo antes do horário o motoboy já pode começar a corrida reservada.", null, "numero", false, 60.0, "min", 0.0, 600.0, 15.0, emptyList()),
    Param("agendada_lembrete_min", "planejadas", "Lembrete ao motoboy", "Aviso no celular de quem reservou, esse tempo antes do horário.", null, "numero", false, 30.0, "min", 5.0, 600.0, 5.0, emptyList()),
    Param("agendada_despacho_auto", "planejadas", "Despachar se ninguém reservar", "Ligado: perto do horário, uma planejada sem reserva vai para o despacho normal (mais próximo).", "Desligado: sem reserva, a corrida fica “sem motoboy” no horário para o gestor resolver.", "booleano", true, 0.0, null, null, null, 1.0, emptyList()),
    Param("agendada_despacho_auto_min", "planejadas", "…quanto tempo antes", "Quando a planejada sem reserva entra no despacho normal.", null, "numero", false, 30.0, "min", 5.0, 600.0, 5.0, emptyList()),
    Param("agendada_atraso_min", "planejadas", "Aviso de atraso", "Se a reservada não for iniciada até esse tempo depois do horário, o gestor e o motoboy são avisados.", null, "numero", false, 10.0, "min", 1.0, 240.0, 1.0, emptyList()),
    Param("agendada_max_por_motoboy", "planejadas", "Reservas por motoboy", "Quantas corridas planejadas cada motoboy pode ter reservadas ao mesmo tempo.", null, "numero", false, 3.0, null, 1.0, 50.0, 1.0, emptyList()),
    Param("agendada_intervalo_min", "planejadas", "Folga entre reservas", "Tempo mínimo entre duas reservas do mesmo motoboy (além da duração prevista da corrida).", null, "numero", false, 30.0, "min", 0.0, 600.0, 5.0, emptyList()),
    Param("exigir_no_local", "local", "Exigir estar no local", "Padrão das novas corridas (pode ser mudado em cada corrida).", null, "booleano", true, 0.0, null, null, null, 1.0, emptyList()),
    Param("raio_chegada_m", "local", "Raio de chegada", "Distância máxima do endereço para marcar chegada e concluir.", null, "numero", false, 150.0, "m", 20.0, 2000.0, 10.0, emptyList()),
    Param("precisao_max_m", "local", "Tolerância ao erro do GPS", "Quanto da imprecisão informada pelo celular é descontada da distância.", null, "numero", false, 80.0, "m", 0.0, 500.0, 10.0, emptyList()),
    Param("posicao_valida_s", "local", "Validade da posição", "Se o app não mandar a posição na hora, usa a última se for mais recente que isso.", null, "numero", false, 180.0, "s", 30.0, 900.0, 1.0, emptyList()),
    Param("repasse_motoboy_pct", "preco", "Repasse ao motoboy", "Parte do valor da corrida que fica com o motoboy. Congelado no aceite. Ajustável por cliente e por corrida.", null, "numero", false, 67.5, "%", 0.0, 100.0, 0.5, emptyList()),
    Param("dev_pct", "preco", "Parte do Dev", "Parte do valor da corrida que vai para o Dev. A gestão fica com o restante (valor − motoboy − Dev). Ajustável por cliente e por corrida.", null, "numero", false, 5.0, "%", 0.0, 100.0, 0.5, emptyList()),
    Param("valor_km_padrao", "preco", "Valor do km (avulso)", "Usado para clientes avulsos e clientes sem valor próprio.", null, "numero", false, 2.0, "R$", 0.0, 100.0, 0.1, emptyList()),
    Param("valor_minimo", "preco", "Valor mínimo", "Nenhuma corrida calculada fica abaixo disso. 0 = sem mínimo.", null, "numero", false, 0.0, "R$", 0.0, 1000.0, 0.5, emptyList()),
    Param("arredondamento", "preco", "Arredondamento", "Arredonda o valor calculado para cima.", null, "opcoes", false, 0.0, null, null, null, 1.0, listOf(0.0 to "Centavos", 0.5 to "R$ 0,50", 1.0 to "R$ 1,00", 5.0 to "R$ 5,00")),
    Param("acrescimo_distancia", "distancia", "Cobrar acréscimo por distância", "Ligado: corridas longas ficam mais caras, pelas faixas abaixo. Cada corrida ainda pode desligar na criação.", "Desligado: o valor da corrida não muda com a distância.", "booleano", true, 0.0, null, null, null, 1.0, emptyList()),
    Param("acrescimo_coleta_ligado", "distancia", "Cobrar distância do motoboy até a coleta", "Ligado: motoboy longe da coleta recebe um valor maior (faixas abaixo).", "Desligado: a distância do motoboy até a coleta não muda o valor.", "booleano", true, 0.0, null, null, null, 1.0, emptyList()),
    Param("acrescimo_entrega_ligado", "distancia", "Cobrar percurso longo", "Ligado: corrida com percurso longo (coleta → entrega) fica mais cara (faixas abaixo).", "Desligado: o tamanho do percurso não muda o valor.", "booleano", true, 0.0, null, null, null, 1.0, emptyList()),
    Param("acrescimo_coleta_cada_km", "distancia", "Coleta longe do motoboy: a cada", "Distância do motoboy até a coleta. Cada motoboy vê o valor com a distância dele. 0 = não cobra.", null, "numero", false, 100.0, "km", 0.0, 5000.0, 10.0, emptyList()),
    Param("acrescimo_coleta_fator", "distancia", "…acrescenta", "Quanto do valor é somado a cada faixa (0,5 = metade do valor).", null, "numero", false, 0.5, "×", 0.0, 10.0, 0.05, emptyList()),
    Param("acrescimo_entrega_cada_km", "distancia", "Entrega longe da coleta: a cada", "Percurso da coleta até a entrega. 0 = não cobra.", null, "numero", false, 200.0, "km", 0.0, 5000.0, 10.0, emptyList()),
    Param("acrescimo_entrega_fator", "distancia", "…acrescenta", "Quanto do valor é somado a cada faixa (0,25 = um quarto do valor).", null, "numero", false, 0.25, "×", 0.0, 10.0, 0.05, emptyList()),
    Param("padrao_exige_nome", "comprovantes", "Nome de quem recebeu", "", null, "booleano", true, 0.0, null, null, null, 1.0, emptyList()),
    Param("padrao_exige_foto", "comprovantes", "Foto", "", null, "booleano", false, 0.0, null, null, null, 1.0, emptyList()),
    Param("padrao_exige_assinatura", "comprovantes", "Assinatura", "", null, "booleano", false, 0.0, null, null, null, 1.0, emptyList()),
    Param("padrao_exige_codigo", "comprovantes", "Código de 4 dígitos", "", null, "booleano", false, 0.0, null, null, null, 1.0, emptyList()),
    Param("intervalo_corrida_s", "rastreamento", "Intervalo em corrida", "De quanto em quanto tempo o app envia a posição durante a corrida.", null, "numero", false, 45.0, "s", 5.0, 300.0, 1.0, emptyList()),
    Param("intervalo_economia_s", "rastreamento", "Intervalo no modo economia", "Quando o motoboy liga a economia de bateria.", null, "numero", false, 90.0, "s", 10.0, 600.0, 1.0, emptyList()),
    Param("disponivel_envio_min", "rastreamento", "Sinal quando disponível", "Envio leve enquanto espera corridas.", null, "numero", false, 3.0, "min", 1.0, 30.0, 1.0, emptyList()),
    Param("disponivel_distancia_m", "rastreamento", "…ou ao se deslocar", "Envia antes do tempo se andar essa distância.", null, "numero", false, 300.0, "m", 50.0, 5000.0, 50.0, emptyList()),
    Param("teste_admin_ignora_local", "teste", "Admin pode ignorar a localização", "Em corridas de teste, contas gestor + motoboy podem avançar etapas fora do local.", null, "booleano", true, 0.0, null, null, null, 1.0, emptyList()),
)
