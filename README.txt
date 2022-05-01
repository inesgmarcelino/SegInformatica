# ProjSInf

## Notas Entrega 2:

- Não conseguimos implementar a verificação das assinaturas
- Policies nao estao a correr corretamente e dao origem a erros na execucao do programa
- Programa funciona se correr dentro do Eclipse, verificamos que existem inconsistencias caso seja utilizado o terminal (não conseguimos achar a fonte do problema) 

## Comandos:

java myAutentClient -u 1 -a ip:23456 -p badpwd -c 2 Jose test
java myAutentClient -u 2 -a ip:23456 -p test -e test.txt
java myAutentClient -u 2 -a ip:23456 -p test -l
java myAutentClient -u 2 -a ip:23456 -p test -d test2.txt

- No ip devemos inserir o ip da máquina onde o servidor está a correr, se for o mesmo que o cliente basta localhost ou 127.0.0.1.

- No exemplo da opção d, vai devolver uma mensagem de erro se  já existir um ficheiro com o mesmo nome do lado do cliente, por isso aconselhamos a criar manualmente um ficheiro test2.txt, e usá-lo na opção d.

----------------------------------------

Como compilar os ficheiros:

	javac src/myAutent.java -d bin
	javac src/myAutentClient.java -d bin


Como correr programa SEM policies:
	
	java -cp bin myAutent
	java -cp bin myAutentClient <comandos>

Como correr programa COM policies:
	
	java -Djava.security.manager -Djava.security.policy=server.policy -cp bin myAutent
	java -Djava.security.manager -Djava.security.policy=client.policy -cp bin myAutentClient <comandos>
