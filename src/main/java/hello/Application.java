package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@SpringBootApplication
@RestController
public class Application {

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

    // find mine position
    PlayerState myState = arenaUpdate.arena.state.get(arenaUpdate._links.self.href);

    // is someone on target
    if (myState.direction.equals("N")) {
        for (Map.Entry<String, PlayerState> entry : arenaUpdate.arena.state.entrySet()) {
            if (entry.getValue().x == myState.x && myState.y - entry.getValue().y <= 3 && myState.y - entry.getValue().y >= 1) {
                return "T";
            }
        }
        if (myState.y > 2) {
            return "F";
        }
    }
    else if (myState.direction.equals("S")) {
        for (Map.Entry<String, PlayerState> entry : arenaUpdate.arena.state.entrySet()) {
            if (entry.getValue().x == myState.x && entry.getValue().y - myState.y <= 3 && entry.getValue().y - myState.y >= 1) {
                return "T";
            }
        }
        if (myState.y < arenaUpdate.arena.dims.get(1) - 2) {
            return "F";
        }
    }
    else if (myState.direction.equals("E")) {
        for (Map.Entry<String, PlayerState> entry : arenaUpdate.arena.state.entrySet()) {
            if (entry.getValue().y == myState.y && entry.getValue().x - myState.x <= 3 && entry.getValue().x - myState.x >= 1) {
                return "T";
            }
        }
        if (myState.x < (arenaUpdate.arena.dims.get(0) - 2)) {
            return "F";
        }
    }
    else if (myState.direction.equals("W")) {
        for (Map.Entry<String, PlayerState> entry : arenaUpdate.arena.state.entrySet()) {
            if (entry.getValue().y == myState.y && myState.x - entry.getValue().x <= 3 && myState.x - entry.getValue().x >= 1) {
                return "T";
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

