# ProjSInf - Projeto 1 e Projeto 2

comandos:
java myAutentClient -u 1 -a ip:23456 -p badpwd -c 2 Jose test
java myAutentClient -u 2 -a ip:23456 -p test -e test.txt
java myAutentClient -u 2 -a ip:23456 -p test -l
java myAutentClient -u 2 -a ip:23456 -p test -d test2.txt

no ip devemos inserir o ip da máquina onde o servidor está a correr, se for o mesmo que o cliente basta localhost ou 127.0.0.1.
no exemplo da opção d, vai devolver uma mensagem de erro se  já existir um ficheiro com o mesmo nome do lado do cliente, por isso aconselhamos a criar manualmente um ficheiro test2.txt, e usá-lo na opção d.