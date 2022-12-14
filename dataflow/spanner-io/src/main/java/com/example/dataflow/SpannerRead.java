/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.dataflow;

import com.google.cloud.spanner.Struct;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.spanner.SpannerIO;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.transforms.Sum;
import org.apache.beam.sdk.transforms.ToString;
import org.apache.beam.sdk.values.PCollection;

/*
This sample demonstrates how to read from a Spanner table.

## Prerequisites
* Maven installed
* Set up GCP default credentials, one of the following:
    - export GOOGLE_APPLICATION_CREDENTIALS=path/to/credentials.json
    - gcloud auth application-default login
  [https://developers.google.com/identity/protocols/application-default-credentials]
* Create the Spanner table to read from, you'll need:
    - Instance ID
    - Database ID
    - Any table, preferably populated
  [https://cloud.google.com/spanner/docs/quickstart-console]

## How to run
cd java-docs-samples/dataflow/spanner-io
mvn clean
mvn compile
mvn exec:java \
    -Dexec.mainClass=com.example.dataflow.SpannerRead \
    -Dexec.args="--instanceId=my-instance-id \
                 --databaseId=my-database-id \
                 --table=my_table \
                 --output=path/to/output_file"
*/
public class SpannerRead {

  public interface Options extends PipelineOptions {

    @Description("Spanner instance ID to query from")
    @Validation.Required
    String getInstanceId();

    void setInstanceId(String value);

    @Description("Spanner database name to query from")
    @Validation.Required
    String getDatabaseId();

    void setDatabaseId(String value);

    @Description("Spanner table name to query from")
    @Validation.Required
    String getTable();

    void setTable(String value);

    @Description("Output filename for records size")
    @Validation.Required
    String getOutput();

    void setOutput(String value);
  }


  public static void main(String[] args) {
    Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
    Pipeline pipeline = Pipeline.create(options);

    String instanceId = options.getInstanceId();
    String databaseId = options.getDatabaseId();
    // [START spanner_dataflow_read]
    // Query for all the columns and rows in the specified Spanner table
    PCollection<Struct> records = pipeline.apply(
        SpannerIO.read()
            .withInstanceId(instanceId)
            .withDatabaseId(databaseId)
            .withQuery("SELECT * FROM " + options.getTable()));
    // [END spanner_dataflow_read]


    PCollection<Long> tableEstimatedSize = records
        // Estimate the size of every row
        .apply(EstimateSize.create())
        // Sum all the row sizes to get the total estimated size of the table
        .apply(Sum.longsGlobally());

    // Write the total size to a file
    tableEstimatedSize
        .apply(ToString.elements())
        .apply(TextIO.write().to(options.getOutput()).withoutSharding());

    pipeline.run().waitUntilFinish();
  }
}
