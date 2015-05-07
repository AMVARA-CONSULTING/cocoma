package com.dai.mif.cocoma.cognos8;

import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BaseClassArrayProp;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.Model;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.Query;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.Report;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.StringArrayProp;
import com.cognos.developer.schemas.bibus._3.StringProp;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.dai.mif.cocoma.cognos.util.C8Access;
import com.dai.mif.cocoma.logging.Logging;

public class CognosUpdateReport {

	private ContentManagerService_PortType cmService = null;
	private Logger log;

	public void updateQueryReport(C8Access c8Access, String reportSearchPath,
			String packageSearchPath) throws Exception {

		this.log = Logging.getInstance().getLog(this.getClass());
		log.debug("--> updatequeryReport() ");

		this.cmService = c8Access.getCmService();
		PropEnum props[] = new PropEnum[] { PropEnum.searchPath,
				PropEnum.metadataModel, PropEnum.metadataModelPackage };

		SearchPathMultipleObject searchPath = new SearchPathMultipleObject();
		searchPath.set_value(reportSearchPath);

		log.debug("searchPath:  [" + searchPath + "]");
		log.debug("packagePath: [" + packageSearchPath + "]");

		BaseClass[] report = this.cmService.query(searchPath, props,
				new Sort[] {}, new QueryOptions());

		if (report.length != 0) {
			log.info("Reports found: " + report.length);
			log.info("each report update may take one or more seconds.");
			if (report.length > 50)
				log.info("please be patient. Set loglevel to DEBUG to see details.");

			String[] percentage_done_liste = new String[10];
			
			for (int i = 0; i < report.length; i++) {

				// Show % of work done already
				int percentage_done = Math.round((float) i / report.length * 10);
				if (percentage_done_liste[percentage_done]==null) {
					log.info(percentage_done+"0% updated");
					percentage_done_liste[percentage_done]="done";
				}

				// do the upgrade
				try {
					if (report[i] instanceof Query
							|| report[i] instanceof Report) {
						// get the search path of the package/model associated
						// to this report or query
						String pkgPath = new String();

						if (report[i] instanceof Report) {
							log.debug("Report (" + i + "/" + report.length
									+ ")");
							pkgPath = ((Report) report[i])
									.getMetadataModelPackage().getValue()[0]
									.getSearchPath().getValue().toString();
						} else {
							log.debug("Query (" + i + "/" + report.length + ")");
							pkgPath = ((Query) report[i])
									.getMetadataModelPackage().getValue()[0]
									.getSearchPath().getValue().toString();

						}
						log.debug("Packagepath: " + pkgPath);
						if (packageSearchPath == null
								|| packageSearchPath.length() == 0
								|| fromPackage(pkgPath, packageSearchPath)) {
							log.debug("Found upgradeable Report|Query");

							log.debug("Will set Package to " + pkgPath
									+ "/model[last()]");
							BaseClassArrayProp bcArrProp2 = new BaseClassArrayProp();
							BaseClass[] value2 = new BaseClass[1];
							StringProp st2 = new StringProp();
							st2.setValue(pkgPath + "/model[last()]");
							value2[0] = new Model();
							value2[0].setSearchPath(st2);
							bcArrProp2.setValue(value2);
							if (report[i] instanceof Query) {
								((Query) report[i])
										.setMetadataModel(bcArrProp2);
							} else
								((Report) report[i])
										.setMetadataModel(bcArrProp2);
							log.debug("MetadataModell was set to last()");

							//
							log.debug("Upgradeing Report|Query");
							cmService.update(new BaseClass[] { report[i] },
									new UpdateOptions());
							log.debug(i + "/" + report.length
									+ " has been updated: "
									+ report[i].getSearchPath().getValue());
						} else {
							log.debug("nothing todo");
						}
					} else
						log.info(report[i].getSearchPath().getValue()
								+ " is not a report or query.");
				} catch (Exception ex) {
					log.error("!!! Exception found");
					log.error("Unable to update object: "
							+ report[i].getSearchPath().getValue());
				}
			}
		} else
			log.info("No sutable query or report found. Please check the searchPath in config, if you think this is an error.");
	}

	public static boolean fromPackage(String rptPackage,
			String comparePackagePath) {
		// determine if the report or query was created against the republished
		// package
		if (rptPackage.equals(comparePackagePath))
			return true;
		else
			return false;
	}
}
