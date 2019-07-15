/* NPC: Heracle
   Function: Guild Creation/Expasion/Disband (GMS-Like)
   Map: 200000301
   Author: Shedder
*/
   
var status = 0;
var sel;

function start() {
    partymembers = cm.getPartyMembers();
    if (cm.getPlayer().getGuildId() > 0) {
        cm.sendSimple( "Então, como posso ajudar?\r\n#b#L0#Eu quero aumentar meu clã#l\r\n#L1#Eu quero desfazer meu clã#l" ); 
    } else {
        cm.sendNext( "Ei... Por acaso você se interessa por CLÃs?"); 
    }
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && status == 1 && sel == 1) {
                cm.sendNext( "Bem pensado. Eu não gostaria de desfazer meu clã que já está tão forte..." ); 
                cm.dispose();
                return;
        }
        if (mode == 0 && status == 0 || mode == 0 && status == 2 && sel == 2  || mode == 0 && status == 2 && sel == 0) {
                cm.dispose();
                return;
        }
        if (mode == 1)
            status++;
        else
            status--;
        if (status == 1) {
            if (cm.getPlayer().getGuildId() > 0) {
                sel = selection;
                if (selection == 0) {
                    if (cm.getPlayer().getGuild().getCapacity() > 95) {
                        cm.sendNext( "Seu clã parece ter crescido um pouco. Eu não posso mais aumentar seu clã." );
                        cm.dispose();		
                    } else {
                        if (cm.getPlayer().getGuildId() < 1 || cm.getPlayer().getGuildRank() != 1) {
                            cm.sendNext( "Ei, você não é o Mestre do Clã!! Esta decisão só pode ser tomada pelo Mestre do Clã." ); 
                            cm.dispose();
                        } else {
                            cm.sendNext( "Você está aqui para aumentar seu clã? Seu clã deve ter crescido um pouco~ Para aumentar seu clã, ele precisa ser recadastrado no nosso Quartel-General de Clãs e isto vai exigir um pagamento pelo serviço..." ); 
                        }
                    }
                } else if (selection == 1) {
                    if (cm.getPlayer().getGuildId() < 1 || cm.getPlayer().getGuildRank() != 1) {
                        cm.sendNext( "Ei, você não é o Mestre do Clã!! Esta decisão só pode ser tomada pelo Mestre do Clã." ); 
                    } else {
                        cm.sendYesNo( "Tem certeza de que deseja desfazer seu clã? Sério... lembre-se, se você desfizer o clã, ele será eliminado para sempre. Ah, e mais uma coisa. Se você quiser desfazer seu clã, vai precisar pagar 200.000 mesos pelo custo do serviço. Ainda quer fazer isto?" ); 
                    }
                }
            } else {
                cm.sendSimple( "#b#L0#O que é um clã?#l\r\n#L1#O que eu faço para criar um clã??#l\r\n#L2# Eu quero criar um clã#l" ); 
            }
        } else if (status == 2) {
            if (cm.getPlayer().getGuildId() > 0) {
                if (cm.getPlayer().getGuildId() > 0 && cm.getPlayer().getGuildRank() == 1) {
                    if (sel == 0) {
                        cm.sendYesNo("O custo do serviço será apenas #r" + cm.getPlayer().getGuild().getIncreaseGuildCost(cm.getPlayer().getGuild().getCapacity()) + " mesos#k Você gostaria de aumentar seu clã?");
                    } else if (sel == 1) {
                        if (cm.getPlayer().getMeso() < 200000) {
                            cm.sendNext( "Ei, você não tem o dinheiro para o serviço... tem certeza de que tem dinheiro suficiente aí?" ); 
                            cm.dispose();
                        } else {
                            cm.getPlayer().disbandGuild();
                            cm.gainMeso(-200000);
                            cm.dispose();
                        }
                    }					
                } else {
                    cm.sendNext( "Ei, você não é o Mestre do Clã!! Esta decisão só pode ser tomada pelo Mestre do Clã." ); 
                    cm.dispose();
                }
            } else {
                sel = selection;
                if (selection == 0) { 
                    cm.sendNext( "Um clã é... bem, você pode pensar nele como um pequeno grupo cheio de pessoas com interesses e objetivos parecidos. Além disto, ele será cadastrado no nosso Quartel-General de Clãs para ser validado." ); 
                    cm.dispose();
                } else if (selection == 1) {
                    cm.sendNext( "Para fazer seu próprio clã, você vai precisar estar, pelo menos, no nível 10. Você também vai precisar ter pelo menos 1.500.000 mesos com você. Este é o preço para registrar seu clã." ); 
                } else if (selection == 2) {
                    cm.sendYesNo( "Certo, agora, você quer criar um clã?" ); 
                }
            }
        } else if (status == 3) {
            if (cm.getPlayer().getGuildId() > 0) {
                if (sel == 0) {
                    cm.getPlayer().increaseGuildCapacity();
                    cm.dispose();
                }
            } else {
                if (sel == 1) {
                    cm.sendNext( "Para fazer um clã, você vai precisar de 6 pessoas no total. Esses 6 devem estar no mesmo grupo e o líder deve vir falar comigo. Fique ciente também de que o líder do grupo também se torna o Mestre do Clã. Uma vez designado o Mestre do Clã, a posição permanece a mesma até que o Clã seja desfeito." ); 
                } else if (sel == 2) {
                    if (cm.getPlayer().getLevel() < 10) {
                        cm.sendNext( "Humm... Eu não acho que você possua as qualificações para ser um Mestre do Clã. Por favor, treine mais para se tornar Mestre do Clã." ); 
                    } else if (cm.getPlayer().getParty() == null) {
                        cm.sendNext( "Eu não me importo com o quão forte você acha que seja... Para formar um clã, você precisa estar em um grupo de 6. Crie um grupo e então traga todos os membros aqui se realmente quiser criar um clã." ); 
                    } else if (!cm.isLeader()) {
                        cm.sendNext( "Você não é o líder de um grupo." ); 
                    } else if (partymembers.size() < 6) {
                        cm.sendNext( "Parece que você não tem membros suficientes no seu grupo ou alguns dos membros não estão presentes. Preciso de todos os 6 membros aqui para cadastrar seu clã. Se seu grupo não consegue coordenar esta simples tarefa, você devia pensar duas vezes antes de formar um clã." ); 
                    } else if (partymembers.get(1).getGuild() != null || partymembers.get(2).getGuild() != null || partymembers.get(3).getGuild() != null || partymembers.get(4).getGuild() != null|| partymembers.get(5).getGuild() != null) {
                        cm.sendNext( "Parece que há um traidor entre nós. Alguém em seu grupo já faz parte de outro clã. Para formar um clã, todos do seu grupo precisam estar sem clã. Volte quando tiver resolvido o problema com o traidor." ); 
                    } else if (cm.isSendContractAvailable(cm.getPlayer().getParty())) {
                        cm.sendNext( "Por favor, aguarde os membros responderem a solicitação para enviar novamente!" ); 
                    } else if (cm.getPlayer().getGuildId() <= 0) {
                        cm.getPlayer().genericGuildMessage(1);
                    } else {
                        cm.sendNext( "Desculpe, mas você não pode criar um clã." ); 
                    }
                    cm.dispose();
                }
            }
        } else if (status == 4) {
            cm.sendNext( "Uma vez que 6 pessoas estejam reunidas, você vai precisar de 1.500.000 mesos. Esse é o preço para registrar seu clã." ); 
        } else if (status == 5) {
            cm.sendNext( "Certo, para registrar seu clã, traga pessoas aqui~ Você não pode criar um sem todos os 6.\r\nAh, é claro, os 6 não podem fazer parte de outro clã!"); 
            cm.dispose();
        }
    }
}

