package hello;

import com.google.api.core.ApiFuture;
import com.google.cloud.ServiceOptions;
import com.google.cloud.bigquery.storage.v1.AppendRowsResponse;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteClient;
import com.google.cloud.bigquery.storage.v1.CreateWriteStreamRequest;
import com.google.cloud.bigquery.storage.v1.JsonStreamWriter;
import com.google.cloud.bigquery.storage.v1.TableName;
import com.google.cloud.bigquery.storage.v1.WriteStream;
import com.google.protobuf.Descriptors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;


@SpringBootApplication
@RestController
public class Application {

    static class WriteCommittedStream {

        final JsonStreamWriter jsonStreamWriter;

        public WriteCommittedStream(String projectId, String datasetName, String tableName) throws IOException, Descriptors.DescriptorValidationException, InterruptedException {

            try (BigQueryWriteClient client = BigQueryWriteClient.create()) {

                WriteStream stream = WriteStream.newBuilder().setType(WriteStream.Type.COMMITTED).build();
                TableName parentTable = TableName.of(projectId, datasetName, tableName);
                CreateWriteStreamRequest createWriteStreamRequest =
                        CreateWriteStreamRequest.newBuilder()
                                .setParent(parentTable.toString())
                                .setWriteStream(stream)
                                .build();

                WriteStream writeStream = client.createWriteStream(createWriteStreamRequest);

                jsonStreamWriter = JsonStreamWriter.newBuilder(writeStream.getName(), writeStream.getTableSchema()).build();
            }
        }

        public ApiFuture<AppendRowsResponse> send(Arena arena) {
            Instant now = Instant.now();
            JSONArray jsonArray = new JSONArray();

            arena.state.forEach((url, playerState) -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("x", playerState.x);
                jsonObject.put("y", playerState.y);
                jsonObject.put("direction", playerState.direction);
                jsonObject.put("wasHit", playerState.wasHit);
                jsonObject.put("score", playerState.score);
                jsonObject.put("player", url);
                jsonObject.put("timestamp", now.getEpochSecond() * 1000 * 1000);
                jsonArray.put(jsonObject);
            });

            return jsonStreamWriter.append(jsonArray);
        }

    }

    final String projectId = ServiceOptions.getDefaultProjectId();
    final String datasetName = "snowball";
    final String tableName = "events";

    final WriteCommittedStream writeCommittedStream;

    public Application() throws Descriptors.DescriptorValidationException, IOException, InterruptedException {
        writeCommittedStream = new WriteCommittedStream(projectId, datasetName, tableName);
    }


    static class Self {
    public String href;
  }

  static class Links {
    public Self self;
  }

  static class PlayerState {
    public Integer x;
    public Integer y;
    public String direction;
    public Boolean wasHit;
    public Integer score;
  }

  static class Arena {
    public List<Integer> dims;
    public Map<String, PlayerState> state;
  }

  static class ArenaUpdate {
    public Links _links;
    public Arena arena;
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.initDirectFieldAccess();
  }

  @GetMapping("/")
  public String index() {
    return "Let the battle begin!";
  }

  @PostMapping("/**")
  public String index(@RequestBody ArenaUpdate arenaUpdate) {
    System.out.println(arenaUpdate);
    writeCommittedStream.send(arenaUpdate.arena);

    // find mine position
    PlayerState myState = arenaUpdate.arena.state.get(arenaUpdate._links.self.href);

    // is someone on target
    if (myState.direction.equals("N")) {
        if (myState.wasHit && myState.y > 2) {
            if (myState.y > 2) {
                return "F";
            } else {
                return "R";
            }
        } else {
            for (Map.Entry<String, PlayerState> entry : arenaUpdate.arena.state.entrySet()) {
                if (entry.getValue().x == myState.x && myState.y - entry.getValue().y <= 3 && myState.y - entry.getValue().y >= 1) {
                    return "T";
                }
            }
        }

        if (myState.y > 2) {
            return "F";
        }
    }
    else if (myState.direction.equals("S")) {
        if (myState.wasHit) {
            if (myState.y < arenaUpdate.arena.dims.get(1) - 2) {
                return "F";
            } else {
                return "R";
            }
        } else {
            for (Map.Entry<String, PlayerState> entry : arenaUpdate.arena.state.entrySet()) {
                if (entry.getValue().x == myState.x && entry.getValue().y - myState.y <= 3 && entry.getValue().y - myState.y >= 1) {
                    return "T";
                }
            }
        }
        if (myState.y < arenaUpdate.arena.dims.get(1) - 2) {
            return "F";
        }
    }
    else if (myState.direction.equals("E")) {
        if (myState.wasHit) {
            if (myState.x < (arenaUpdate.arena.dims.get(0) - 2)) {
                return "F";
            } else {
                return "R";
            }
        } else {
            for (Map.Entry<String, PlayerState> entry : arenaUpdate.arena.state.entrySet()) {
                if (entry.getValue().y == myState.y && entry.getValue().x - myState.x <= 3 && entry.getValue().x - myState.x >= 1) {
                    return "T";
                }
            }
        }
        if (myState.x < (arenaUpdate.arena.dims.get(0) - 2)) {
            return "F";
        }
    }
    else if (myState.direction.equals("W")) {
        if (myState.wasHit) {
            if (myState.x > 2) {
                return "F";
            } else {
                return "R";
            }
        } else {
            for (Map.Entry<String, PlayerState> entry : arenaUpdate.arena.state.entrySet()) {
                if (entry.getValue().y == myState.y && myState.x - entry.getValue().x <= 3 && myState.x - entry.getValue().x >= 1) {
                    return "T";
                }
            }
        }
        if (myState.x > 2) {
            return "F";
        }
    }

    return "R";

    // if (myState.x < arenaUpdate.arena.dims.get(0))
    // String[] commands = new String[]{"F", "R", "L"};
    // int i = new Random().nextInt(3);
    // return commands[i];
  }

}

