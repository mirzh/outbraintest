# outbraintest

## Running the project
1) start the server: run the server's main class in: 
```
src/main/scala/com/outbraintest/server/ServerMain.scala
```

2) start the client: run the client's main class in:
```
src/main/scala/com/outbraintest/client/ClientMain.scala
```
you may provide the number of clients as the first arg (args(0)) otherwise you will be promped to enter a number when the application starts.

## Solution explanation
### Client
In order to manage the clients I created an actor for each client. 
Each actor is sending a request to the server and waits a random time between 0 and 2000 milliseconds before sending again.
Clicking on the ENTER button will stop the actors from sending messages and terminate the actor system.

I used akka http client to send the http requests.

### Server
The server's state is manage by a single actor in a mutable hashmap.
The reason I chose this approach is to provide thread safety. 
The hashmap is accessed only via the actors messages and therefore it is guaranteed that only one thread will access the map each time.  

I used akka http server to provide rest api.
