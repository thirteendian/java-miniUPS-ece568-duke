package edu.duke.ece568.client;

import edu.duke.ece568.client.database.Shipment;
import edu.duke.ece568.client.database.ShipmentRepository;
import edu.duke.ece568.shared.Status;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Controller
public class HelloController {
    private final ShipmentRepository repository;
    private Boolean isSignUp = false;


    public HelloController(ShipmentRepository repository) {
        this.repository = repository;
    }

//    @PostMapping("/information")
//    public String information(@ModelAttribute Information information, Model model) {
//        Shipment shipment = repository.findPackageByTrackingNum(information.getTrackingNum());//"3ac02869-91c9-47f6-93b6-5a5c06bf2a29");
//        information.setShipment(shipment);
//        model.addAttribute("information",information);
//        return "information";
//    }


//    @GetMapping("/signup")
//    public String signup(Model model) {
//        model.addAttribute("signup",new Signup());
//        return "";
//    }
//

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("signup", new Signup());
        return "signup";
    }

    @PostMapping("/signup")
    public String signupPost(@ModelAttribute Signup signup, Model model) {
        model.addAttribute("signup", signup);
        this.isSignUp = true;
        return "signupsuccess";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("login", new Login());
        return "login";
    }

    @PostMapping("/login")
    public String loginpost(@ModelAttribute Login login, Model model) {
        model.addAttribute("login", login);
        this.isSignUp = true;
        return "signupsuccess";
    }

    @GetMapping("/information")
    public String information(Model model) {
        model.addAttribute("information", new Information());
        return "information";
    }

    @PostMapping("/information")
    public String greetingSubmit(@ModelAttribute Information information, Model model) {
        Shipment shipment = repository.findPackageByTrackingNum(information.getTrackingNum());//"3ac02869-91c9-47f6-93b6-5a5c06bf2a29");
        information.setShipment(shipment);
        information.setStatus(new Status().getStatus(shipment.getStatus()));
        model.addAttribute("information", information);
        if (this.isSignUp) {
            return "result";
        } else return "resultnotsignup";
    }

    @GetMapping("/update")
    public String update(Model model) {
        if (isSignUp) {
            model.addAttribute("newdest", new Newdest());
            return "updatestatus";
        }else return "cantchangenotlogin";
    }

    @PostMapping("/update")
    public String updateMethod(@ModelAttribute Newdest newdest, Model model) {
        model.addAttribute("newdest", newdest);
        Shipment shipment = repository.findPackageByTrackingNum(newdest.getTrackingNum());

        if (shipment.getStatus() == new Status().pInWarehouse) {
            repository.setShipment_x(newdest.getX_dest(), shipment.getPackage_id());
            repository.setShipment_y(newdest.getY_dest(), shipment.getPackage_id());
            return "updateSuccess";
        } else {
            return "updateFail";
        }

    }


}

