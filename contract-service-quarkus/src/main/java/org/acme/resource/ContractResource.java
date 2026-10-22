package org.acme.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.client.PropertyRestClient;
import org.acme.dto.CreateContractRequest;
import org.acme.dto.RoomDTO;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Map;

@Path("/api/v1/contracts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContractResource {

    @Inject
    @RestClient
    PropertyRestClient propertyRestClient;

    @POST
    public Response createContract(CreateContractRequest request) {
        RoomDTO room;

        try {
            room = propertyRestClient.getRoomById(request.getRoomId());
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Phòng không tồn tại"))
                        .build();
            }
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Lỗi kết nối tới Property Service"))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Không thể kết nối đến Property Service (Service Down)"))
                    .build();
        }

        if (room == null || !"AVAILABLE".equalsIgnoreCase(room.getStatus())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Phòng " + request.getRoomId() + " đã có người thuê hoặc không khả dụng!"))
                    .build();
        }
        try{
            propertyRestClient.updateRoomStatus(request.getRoomId(),Map.of("status","OCCUPIED"));
            room.setStatus("OCCUPIED");
        }catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Không thể cập nhật trạng thái phòng: " + e.getMessage()))
                    .build();
        }
        long generatedContractId = System.currentTimeMillis();
        return Response.status(Response.Status.CREATED)
                .entity(Map.of(
                        "message", "Tạo hợp đồng thành công từ Quarkus Consumer!",
                        "contractId", generatedContractId,
                        "room", room
                ))
                .build();
    }
}