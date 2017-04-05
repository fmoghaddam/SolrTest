import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.noggit.JSONUtil;

public class SolrSearcher {
	private static final Logger LOG = Logger.getLogger(SolrSearcher.class.getCanonicalName());
	private static final String SOLR_IP = "http://141.66.13.10:8983/solr/nyt_corpus";
	private static final String RESULT_FOLDER = "data";
	private static final String ENTITIES_NAME_FOLDER="entityfile";
	
	private static final Map<String,Long> COUNT_PER_ENTITY = new HashMap<>();
	private static final Map<String,Long> COUNT_PER_FILE = new HashMap<>();

	public static void main(String[] args) {
		try{
			final HttpSolrClient solr = new HttpSolrClient(SOLR_IP);
			solr.setParser(new XMLResponseParser());

			final File[] listOfFiles = new File(ENTITIES_NAME_FOLDER).listFiles();

			for (int i = 0; i < listOfFiles.length; i++) {
				final String fineName = listOfFiles[i].getName();

				final BufferedReader br = new BufferedReader(new FileReader(ENTITIES_NAME_FOLDER+File.separator+fineName));
				final Set<String> alreadySeen = new HashSet<String>();
				String entityName;
				while ((entityName= br.readLine()) != null){
					entityName = URLUTF8Encoder.unescape(entityName);

					entityName = StringUtils.native2Ascii(entityName);
					entityName = entityName.replace("\\","\\\\");
					final String queryString = "entities:"+"\"YAGO:"+entityName+"\"";
					final SolrQuery qry = new SolrQuery(queryString);
					qry.setRows(2147483647);
					qry.setRequestHandler("select");
					final QueryRequest qryReq = new QueryRequest(qry);
					final QueryResponse resp = qryReq.process(solr);
					final SolrDocumentList results = resp.getResults();
					long count = results.size();

					COUNT_PER_FILE.merge(fineName, count, Long::sum);
					COUNT_PER_ENTITY.merge(entityName, count, Long::sum);
					
					for (int j = 0; j < count; j++) {
						final SolrDocument hitDoc = results.get(j);
						final String url = (String) hitDoc.getFieldValue("url");
						final String id = (String) hitDoc.getFieldValue("id");
						if(alreadySeen.contains(url)){
							continue;
						}
						final String jsonResult = JSONUtil.toJSON(hitDoc);
						final String fileNameText = fineName+File.separator+entityName+File.separator+id;
						writeToFile(fileNameText,jsonResult);
						alreadySeen.add(url);
					}
				}
				br.close();
				printStatistic();
				COUNT_PER_FILE.clear();
				COUNT_PER_ENTITY.clear();
			}
		}catch(final Exception exception) {
			LOG.error(exception.getMessage());
		}
	}

	private static void printStatistic() {
		for(final Entry<String, Long> entry: COUNT_PER_FILE.entrySet()){
			LOG.info("=SPLIT(\""+entry.getKey()+","+entry.getValue()+"\",\",\")");
		}
		
		for(final Entry<String, Long> entry: COUNT_PER_ENTITY.entrySet()){
			LOG.info("=SPLIT(\""+entry.getKey()+","+entry.getValue()+"\",\",\")");
		}
		LOG.info("----------------------------------------------------------------");
	}

	private static void writeToFile(final String fileName, final String jsonResult) throws IOException {
		FileUtils.writeStringToFile(new File(RESULT_FOLDER +File.separator+fileName), jsonResult, Charset.defaultCharset());
	}
}