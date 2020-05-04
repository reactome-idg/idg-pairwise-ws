# The following box plot code is based on this web page: 
# https://www.r-bloggers.com/add-p-values-and-significance-levels-to-ggplots/
require("ggpubr")
setwd("/Users/wug/git/reactome-idg/idg-pairwise/examples")
pdf("plots.pdf", onefile = TRUE)
merged.data <- read.delim("Corr_Query_0202_Merged.txt")
compar.results <- compare_means(SpearmCorr ~ DataSource, data = merged.data)
print(compar.results)
p <- ggboxplot(merged.data, x = "DataSource", xlab = "Data Source", y = "SpearmCorr", ylab = "Spearrman Correlation",
               color = "DataSource",
               labels = pairs[i],
               palette = "jco", add = "jitter",
               title = "All")
print(p)
pairs <- sort(unique(merged.data$Pair))
for (i in 1 : length(pairs)) {
    print(pairs[i])
    which <- merged.data$Pair == pairs[i]
    filtered <- merged.data[which, ]
    compar.results <- compare_means(SpearmCorr ~ DataSource, data = filtered)
    print(compar.results)
    p <- ggboxplot(filtered, x = "DataSource", xlab = "Data Source", y = "SpearmCorr", ylab = "Spearrman Correlation",
                   color = "DataSource",
                   title = pairs[i],
                   palette = "jco", add = "jitter")
    p + stat_compare_means()
    # boxplot(SpearmCorr ~ DataSource, data = filtered)
    print(p)
    # break
}
dev.off()
