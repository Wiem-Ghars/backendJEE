FROM tomcat:10-jdk17
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY target/yourapp.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]