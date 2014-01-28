filename <- list.files(path = "./stat", pattern ="^stat.*.csv", all.files = FALSE,
           full.names = TRUE, recursive = FALSE)

fi <- as.data.frame(filename)

fi["conc"] <- NA

fi$conc <-  sub("./stat/stat([0-9]+).csv", "\\1", fi$filename ,perl=TRUE)

for (i in 1:length(fi$filename)) {

    data <- read.table(toString(fi$filename[i]), header=TRUE, sep=",")

    data$roundtime <- as.integer(data$time)

    rpsframe <- aggregate( data$roundtime, list(data$roundtime), FUN = length)

    colnames(rpsframe) <- c("roundtime", "freq")

    data <- merge(data, rpsframe, by="roundtime", all=TRUE)

    concurrency <- fi$conc[i]

    rps <- round(length(data$time)/max(data$time), digits = 0)

    quant <- quantile(data$latency, c(0.99))[1]

    roundquant <- round(quant, digits = 2)

    baselatency <- subset(data, latency <= quant, select=c(latency))

    slowlatency <- subset(data, latency > quant, select=c(latency))

    png(paste("./stat/plot",toString(concurrency),".png", sep = ""), width = 1000, height = 1000, res=120)

    par(mfrow=c(3,2),xpd=NA, oma = c( 4, 0, 4, 0 ) )


    ## Histogram of remote actor latency, 99% of requests
    hist(
        baselatency$latency,
        main = "Histogram of remote actor latency, 99% of requests",
        xlab = "Latency, ms",
        yaxt = "n",
        sub = paste("requests latency <= ",toString(roundquant)," ms", sep = ""),
        breaks = 9,
        labels = TRUE,
        freq = TRUE
    )

    axis(
        side = 2,
        at=axTicks(2),
        labels=format(axTicks(2),scientific = FALSE)
    )

    leg1 <- paste("mean = ", round(mean(baselatency$latency), digits = 4))
    leg2 <- paste("sd = ", round(sd(baselatency$latency),digits = 4))
    count <- paste("count = ", length(baselatency$latency))
    legend(x = "topright", c(leg1,leg2,count), bty = "n")


    ## Histogram of remote actor latency, 1% of requests
    hist(
        slowlatency$latency,
        main="Histogram of remote actor latency, 1% of requests",
        xlab="Latency, ms",
        yaxt="n",
        sub=paste("requests latency > ",toString(roundquant)," ms", sep = ""),
        labels=TRUE,
        freq = TRUE
    )

    axis(
        side=2,
        at=axTicks(2),
        labels=format(axTicks(2),scientific=FALSE)
    )

    count <- paste("count = ", length(slowlatency$latency))
    legend(x = "topright", c(count), bty = "n")


    ## Latency in time
    plot(
        data$time,
        data$latency,
        type="l",
        main="Latency in time",
        ylab="Latency, ms",
        xlab="Time from start, s"
    )

    ## Requests per second
    plot(
        data$time,
        data$freq,
        type="l",
        main="Requests per second",
        ylab="rps",
        xlab="Time from start, s"
    )


    ## Pong actor mailbox size in time
    plot(
        data$time,
        data$pongrcvmailboxsize,
        type="l",
        main="Pong actor mailbox size in time",
        ylab="Mailbox size",
        xlab="Time from start, s"
    )


    ## Ping actor mailbox size in time
    plot(
        data$time,
        data$pingrcvmailboxsize,
        type="l",
        main="Ping actor mailbox size in time",
        ylab="Mailbox size",
        xlab="Time from start, s"
    )


    ## Main title
    title(
        main = "Remote ping-pong actor latency test",
        outer=TRUE
    )

    ## Main subtitle
    mtext(
        side=3,
        outer=TRUE,
        cex = 0.8,
        paste(
            "concurrency = ",
            toString(concurrency),
            ", count = ",
            toString(length(data$latency)),
            ", rps = ",
            toString(rps)
        )
    )

    dev.off()

}

alldata <- data.frame(id= integer(0), time =numeric(0), latency=numeric(0), pongrcvmailboxsize=integer(0), pingrcvmailboxsize=integer(0), concurrency=integer(0))

for (i in 1:length(fi$filename)) {

    data <- read.table(toString(fi$filename[i]), header=TRUE, sep=",")
    alldata <- rbind(alldata,  data)

}

alldata$roundtime <- as.integer(alldata$time)

rpsframe <- aggregate( id ~ concurrency+roundtime,data = alldata,FUN=length )

colnames(rpsframe) <- c("concurrency", "roundtime", "freq")

alldata <- merge(alldata, rpsframe, by=c("roundtime","concurrency"), all=TRUE)

png(paste("./stat/plot.png"), width = 600, height = 1000, res = 120)

par(mfrow=c(2,1))

boxplot(latency~concurrency,data=alldata, main="Latency versus Concurrency",
  	 xlab="Concurrency, number of parallel requests", ylab="Latency, ms", log = "y", outline=FALSE)

boxplot(freq~concurrency,data=alldata, main="Rps versus Concurrency",
  	 xlab="Concurrency, number of parallel requests", ylab="Rps", outline=FALSE)

dev.off()