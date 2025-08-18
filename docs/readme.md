

Starting webgoat docker container in srv-vm-ubuntu, we will use the port 8081 to avoid collision with the 8080 

```bash
docker run -d -p 0.0.0.0:8080:8081 -p 0.0.0.0:9090:9090 webgoat/webgoat
``` 

then we can access webgoat by going to 
http://srv-vm-ubuntu:8080/WebGoat 

and the admin console at 
http://localhost:9090/WebGoat/admin

to lunch chromium web browser pointing to the proxy already running from ZAP, this is the command 

```bash
chromium --proxy-server=127.0.0.1:8099
```


# Notes
Intercepted cookies.

6144753438991004996-1753008309327
6144753438991004997-1753008420529
6144753438991004998-1753008432183

6144753438991004999-1753008432183

6144753438991005000-1753008445121