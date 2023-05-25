package datnnhom12api.service.impl;

import datnnhom12api.core.Filter;
import datnnhom12api.dto.*;
import datnnhom12api.entity.*;
import datnnhom12api.exceptions.CustomException;
import datnnhom12api.jwt.user.CustomUserDetails;
import datnnhom12api.repository.*;
import datnnhom12api.request.*;
import datnnhom12api.service.OrderService;
import datnnhom12api.dto.specifications.OrderSpecifications;
import datnnhom12api.utils.support.OrderDetailStatus;
import datnnhom12api.utils.support.ReturnDetailStatus;
import datnnhom12api.utils.support.ReturnStatus;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service("orderService")
@Transactional(rollbackFor = Throwable.class)
public class OrderServiceImpl implements OrderService {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderDetailRepository orderDetailRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    InformationRepository informationRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ExchangeDetailRepository exchangeDetailRepository;

    @Autowired
    private ExchangeRepository exchangeRepository;

    @Override
    public OrderEntity save(OrderRequest orderRequest) throws CustomException {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setData(orderRequest);
        if (orderRequest.getUserId() == 0) {
            orderEntity.setUser(null);
        } else {
            UserEntity userEntity = userRepository.getById(orderRequest.getUserId());
            orderEntity.setUser(userEntity);
        }
        CustomUserDetails authentication1 = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        System.out.println("Tài khoàn đang đăng nhập" + authentication1.getUsername());
        orderEntity = orderRepository.save(orderEntity);
        OrderHistoryEntity orderHistory = new OrderHistoryEntity();
        orderHistory.setOrderId(orderEntity);
        orderHistory.setVerifier(authentication1.getUsername());
        orderHistory.setTotal(orderEntity.getTotal());
        orderHistory.setStatus(String.valueOf(orderEntity.getStatus()));
        this.orderHistoryRepository.save(orderHistory);
        List<OrderDetailRequest> list = orderRequest.getOrderDetails();
        for (OrderDetailRequest orderDetailRequest : list) {
            OrderDetailEntity orderDetailEntity = new OrderDetailEntity();
            orderDetailEntity.setData(orderDetailRequest);
            orderDetailEntity.setProduct(this.productRepository.getById(orderDetailRequest.getProductId()));
            orderDetailEntity.setOrder(orderEntity);
            orderDetailRepository.save(orderDetailEntity);
        }
        List<CartEntity> listCard = this.cartRepository.findAll();
        if (!orderRequest.getStatus().equals("CHUA_THANH_TOAN")) {
            this.cartRepository.deleteAll(listCard);
        }

        for (OrderDetailRequest orderDetailEntity : orderRequest.getOrderDetails()) {
            ProductEntity product = this.productRepository.getById(orderDetailEntity.getProductId());
            product.setQuantity(product.getQuantity() - orderDetailEntity.getQuantity());
            this.productRepository.save(product);
        }


        return orderEntity;
    }
    @Override
    public OrderEntity saveNoLogin(OrderRequest orderRequest) throws CustomException {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setData(orderRequest);
        UserEntity userEntity = null;
        orderEntity.setUser(userEntity);
        orderEntity = orderRepository.save(orderEntity);
        List<OrderDetailRequest> list = orderRequest.getOrderDetails();
        for (OrderDetailRequest orderDetailRequest : list) {
            OrderDetailEntity orderDetailEntity = new OrderDetailEntity();
            orderDetailEntity.setData(orderDetailRequest);
            orderDetailEntity.setProduct(this.productRepository.getById(orderDetailRequest.getProductId()));
            orderDetailEntity.setOrder(orderEntity);
            orderDetailRepository.save(orderDetailEntity);
        }
        for (OrderDetailRequest orderDetailEntity : orderRequest.getOrderDetails()) {
            System.out.println(orderDetailEntity.getProductId());
            ProductEntity product = this.productRepository.getById(orderDetailEntity.getProductId());
            product.setQuantity(product.getQuantity() - orderDetailEntity.getQuantity());
            this.productRepository.save(product);
        }
        OrderHistoryEntity orderHistory = new OrderHistoryEntity();
        orderHistory.setStatus(String.valueOf(orderEntity.getStatus()));
        orderHistory.setOrderId(orderEntity);
        orderHistory.setTotal(orderEntity.getTotal());
        orderHistory.setVerifier(null);
        this.orderHistoryRepository.save(orderHistory);
        return orderEntity;
    }

    @Override
    public OrderEntity saveOfUser(OrderRequest orderRequest) throws CustomException {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setData(orderRequest);
        UserEntity userEntity = userRepository.getById(orderRequest.getUserId());
        orderEntity.setUser(userEntity);
        orderEntity = orderRepository.save(orderEntity);
        List<OrderDetailRequest> list = orderRequest.getOrderDetails();
        for (OrderDetailRequest orderDetailRequest : list) {
            OrderDetailEntity orderDetailEntity = new OrderDetailEntity();
            orderDetailEntity.setData(orderDetailRequest);
            orderDetailEntity.setProduct(this.productRepository.getById(orderDetailRequest.getProductId()));
            orderDetailEntity.setOrder(orderEntity);
            orderDetailRepository.save(orderDetailEntity);
        }
        for (OrderDetailRequest orderDetailEntity : orderRequest.getOrderDetails()) {
            System.out.println(orderDetailEntity.getProductId());
            ProductEntity product = this.productRepository.getById(orderDetailEntity.getProductId());
            product.setQuantity(product.getQuantity() - orderDetailEntity.getQuantity());
            this.productRepository.save(product);
        }
        OrderHistoryEntity orderHistory = new OrderHistoryEntity();
        CustomUserDetails authentication1 = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        System.out.println("Tài khoàn đang đăng nhập" + authentication1.getUsername());
        orderHistory.setStatus(String.valueOf(orderEntity.getStatus()));
        orderHistory.setOrderId(orderEntity);
        orderHistory.setTotal(orderEntity.getTotal());
        orderHistory.setVerifier(authentication1.getUsername());
        this.orderHistoryRepository.save(orderHistory);
        return orderEntity;
    }

    @Override
    public OrderEntity edit(Long id, OrderRequest orderRequest) throws CustomException {
        Optional<OrderEntity> orderEntityOptional = orderRepository.findById(id);
        if (id <= 0) {
            throw new CustomException(403, "id đơn hàng phải lớn hơn 0");
        }
        if (orderEntityOptional.isEmpty()) {
            throw new CustomException(403, "không tìm thấy id đơn hàng muốn sửa");
        }
        OrderEntity orderEntity = orderEntityOptional.get();
        orderEntity.setData(orderRequest);
        orderEntity = orderRepository.save(orderEntity);
        String status = String.valueOf(orderEntity.getStatus());
        Long orderId = orderEntity.getId();
        if (orderRequest.getOrderDetails().get(0).getProductId() != null) {
            orderRequest.getOrderDetails().forEach(orderDetailRequest -> {
                OrderDetailEntity orderDetailEntity = new OrderDetailEntity();
                orderDetailEntity = this.orderDetailRepository.
                        getOrderDetailEntityByOrderIsAndAndProduct(orderId, orderDetailRequest.getProductId());
                System.out.println(orderDetailEntity.getProduct() + " " + orderDetailEntity.getQuantity() + orderDetailEntity.getTotal());
                orderDetailEntity.setId(orderDetailEntity.getId());
                orderDetailEntity.setStatus(OrderDetailStatus.valueOf(status));
                orderDetailEntity.setTotal(orderDetailEntity.getProduct().getPrice() * orderDetailRequest.getQuantity());
                orderDetailEntity.setProduct(orderDetailEntity.getProduct());
                orderDetailEntity.setIsCheck(orderDetailRequest.getIsCheck());
                orderDetailEntity.setQuantity(orderDetailRequest.getQuantity());
                orderDetailEntity.setOrder(orderDetailEntity.getOrder());
                this.orderDetailRepository.save(orderDetailEntity);
            });
            double count = 0;
            List<OrderDetailEntity> list = this.orderDetailRepository.getOrderDetailEntityById(orderId);
            for (OrderDetailEntity od : list) {
                count += od.getTotal();
            }
            orderEntity.setTotal(count + orderRequest.getShippingFree());
            this.orderRepository.save(orderEntity);

            System.out.println("cập nhật lại lịch sử đơn hàng");
            CustomUserDetails authentication1 = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            OrderHistoryEntity orderHistory = new OrderHistoryEntity();
            orderHistory.setStatus(orderRequest.getStatus());
            orderHistory.setOrderId(orderEntity);
            orderHistory.setTotal(orderEntity.getTotal());
            orderHistory.setVerifier(authentication1.getUsername());
            this.orderHistoryRepository.save(orderHistory);
        } else {
            List<OrderDetailEntity> list = this.orderDetailRepository.getOrderDetailEntityById(orderEntity.getId());
            list.forEach(
                    list1 -> {
                        list1.setId(list1.getId());
                        list1.setTotal(list1.getTotal());
                        list1.setProduct(list1.getProduct());
                        list1.setStatus(OrderDetailStatus.valueOf(status));
                        list1.setQuantity(list1.getQuantity());
                        OrderEntity order = orderRepository.getById(orderId);
                        list1.setOrder(order);
                    });
            this.orderDetailRepository.saveAll(list);
        }

        return orderEntity;
    }

    @Override
    public OrderEntity delete(Long id) throws CustomException {
        Optional<OrderEntity> orderEntityOptional = orderRepository.findById(id);
        if (orderEntityOptional.isEmpty()) {
            throw new CustomException(403, "không tìm thấy đơn hàng");
        }
        OrderEntity orderEntity = orderRepository.getById(id);
        orderEntity.getOrderDetails().forEach(orderDetailEntity -> {
            ProductEntity product = this.productRepository.getById(orderDetailEntity.getProduct().getId());
            product.setQuantity(product.getQuantity() + orderDetailEntity.getQuantity());
            this.productRepository.save(product);
        });
        orderRepository.delete(orderEntity);
        return orderEntity;
    }

    @Override
    public Page<OrderEntity> paginate(String searchPayment, String searchName, String searchStatus, String searchPhone, String startDate, String endDate, int page, int limit, List<Filter> filters, Map<String, String> sortBy) {
        List<Sort.Order> orders = new ArrayList<>();
        for (String field : sortBy.keySet()) {
            orders.add(new Sort.Order(Sort.Direction.fromString(sortBy.get(field)), field));
        }
        Sort sort = orders.size() > 0 ? Sort.by(orders) : Sort.by("id").descending();
        Pageable pageable = PageRequest.of(page, limit, sort);
        Specification<OrderEntity> specifications = OrderSpecifications.getInstance().getQueryResult(filters);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (!searchStatus.isEmpty() && !searchPayment.isEmpty() && !searchName.isEmpty() && !searchPhone.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDateAndPhoneAndStatusAndPaymentAndName(searchPayment, searchName, searchStatus, searchPhone, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchStatus.isEmpty() && !searchPayment.isEmpty() && !searchPhone.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDateAndPhoneAndStatusAndPayment(searchPayment, searchStatus, searchPhone, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchStatus.isEmpty() && !searchPayment.isEmpty() && !searchName.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDateAndNameAndStatusAndPayment(searchPayment, searchStatus, searchName, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchStatus.isEmpty() && !searchName.isEmpty() && !searchPhone.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDateAndPhoneAndStatusAndName(searchName, searchStatus, searchPhone, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchStatus.isEmpty() && !searchPhone.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDateAndPhoneAndStatus(searchStatus, searchPhone, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchStatus.isEmpty() && !searchPayment.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDateAndPaymentAndStatus(searchStatus, searchPayment, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchStatus.isEmpty() && !searchName.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDateAndNameAndStatus(searchStatus, searchName, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchStatus.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDateAndStatus(searchStatus, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchStatus.isEmpty() && !searchName.isEmpty() && !searchPhone.isEmpty()) {
            return orderRepository.searchNameAndPhoneAndStatus(searchStatus, searchName, searchPhone, specifications, pageable);
        } else if (!searchStatus.isEmpty() && !searchName.isEmpty() && !searchPayment.isEmpty()) {
            return orderRepository.searchNameAndPaymentAndStatus(searchStatus, searchName, searchPayment, specifications, pageable);
        } else if (!searchStatus.isEmpty() && !searchPayment.isEmpty() && !searchPhone.isEmpty()) {
            return orderRepository.searchPaymentAndPhoneAndStatus(searchStatus, searchPayment, searchPhone, specifications, pageable);
        } else if (!searchStatus.isEmpty() && !searchName.isEmpty()) {
            return orderRepository.searchNameAndStatus(searchStatus, searchName, specifications, pageable);
        } else if (!searchStatus.isEmpty() && !searchPayment.isEmpty()) {
            return orderRepository.searchPaymentAndStatus(searchStatus, searchPayment, specifications, pageable);
        } else if (!searchStatus.isEmpty() && !searchPhone.isEmpty()) {
            return orderRepository.searchPhoneAndStatus(searchStatus, searchPhone, specifications, pageable);
        } else if (!searchStatus.isEmpty() && !startDate.isEmpty()) {
            return orderRepository.betweenDateAndStatus(searchStatus, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchStatus.isEmpty()) {
            return orderRepository.findAllAndStatus(searchStatus, specifications, pageable);
        } else if (searchPayment.isEmpty() && !searchName.isEmpty() && !searchPhone.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDateAndPhoneAndPaymentAndName(searchPayment, searchName, searchPhone, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchPayment.isEmpty() && !searchPhone.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDateAndPhoneAndPayment(searchPayment, searchPhone, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchPayment.isEmpty() && !searchName.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDateAndNameAndPayment(searchPayment, searchName, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchName.isEmpty() && !searchPhone.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDateAndPhoneAndName(searchName, searchPhone, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchName.isEmpty() && !searchPhone.isEmpty()) {
            return orderRepository.searchNameAndPhone(searchName, searchPhone, specifications, pageable);
        } else if (!searchName.isEmpty() && !searchPayment.isEmpty()) {
            return orderRepository.searchNameAndPayment(searchName, searchPayment, specifications, pageable);
        } else if (!searchPhone.isEmpty() && !searchPayment.isEmpty()) {
            return orderRepository.searchPhoneAndPayment(searchPhone, searchPayment, specifications, pageable);
        } else if (!searchPhone.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDateAndPhone(searchPhone, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchPayment.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDateAndPayment(searchPayment, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchName.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDateAndName(searchName, LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!startDate.isEmpty() && !endDate.isEmpty()) {
            return orderRepository.betweenDate(LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else if (!searchName.isEmpty()) {
            return orderRepository.searchName(searchName, specifications, pageable);
        } else if (!searchPayment.isEmpty()) {
            return orderRepository.searchPayment(searchPayment, specifications, pageable);
        } else if (!searchPhone.isEmpty()) {
            return orderRepository.searchPhone(searchPhone, specifications, pageable);
        } else if (!startDate.isEmpty()) {
            return orderRepository.betweenDate(LocalDateTime.parse(startDate, dateTimeFormatter), LocalDateTime.parse(endDate, dateTimeFormatter), specifications, pageable);
        } else {
            return orderRepository.findAll(specifications, pageable);
        }
    }

    @Override
    public UserEntity createUser(CreateUserOnOrderRequest createUserOnOrderRequest) throws CustomException {
        List<UserEntity> userEntityList = userRepository.findAll();
        for (UserEntity userEntity : userEntityList) {
            if (createUserOnOrderRequest.getUsername().equals(userEntity.getUsername())) {
                throw new CustomException(403, "Tài khoản đã tồn tại!");
            }
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(createUserOnOrderRequest.getUsername());
        BCryptPasswordEncoder b = new BCryptPasswordEncoder();
        userEntity.setPassword(b.encode(createUserOnOrderRequest.getNewPassword()));
        userEntity.setStatus(1);
        List<RoleEntity> roleEntityList = new ArrayList<>();
        RoleEntity roleEntity = userRepository.findRoleCustomer();
        roleEntityList.add(roleEntity);
        userEntity.setRoles(roleEntityList);
        userEntity = userRepository.save(userEntity);
        InformationEntity informationEntity = new InformationEntity();
        informationEntity.setFullName(createUserOnOrderRequest.getFullName());
        informationEntity.setEmail(createUserOnOrderRequest.getEmail());
        informationEntity.setPhoneNumber(createUserOnOrderRequest.getPhoneNumber());
        informationEntity.setAddress(createUserOnOrderRequest.getAddress());
        informationEntity.setUser(userEntity);
        informationEntity.setActive(1);
        informationRepository.save(informationEntity);
        return userEntity;
    }

    @Override
    public List<OrderDetailDTO> findByOrder(Long id) {
        List<OrderDetailEntity> orderDEntity = this.orderDetailRepository.findByOrder(id);
        this.enrichProduct(orderDEntity);
        ModelMapper modelMapper = new ModelMapper();
        List<OrderDetailDTO> dtos = orderDEntity
                .stream()
                .map(user -> modelMapper.map(user, OrderDetailDTO.class))
                .collect(Collectors.toList());

        return dtos;

    }

    private void enrichProduct(List<OrderDetailEntity> orderDEntity) {
        orderDEntity.forEach(orderDetailEntity -> {
            ProductEntity product = this.productRepository.getById(orderDetailEntity.getProduct().getId());
            orderDetailEntity.enrichProduct(product);
        });
    }

    @Override
    public OrderByIdDTO findById(Long id) {
        OrderEntity orderEntity = this.orderRepository.getById(id);
        ModelMapper modelMapper = new ModelMapper();
        OrderByIdDTO orderDTO = modelMapper.map(orderEntity, OrderByIdDTO.class);
        return orderDTO;
    }

    @Override
    public OrderEntity cancelled(Long id) throws CustomException {
        Optional<OrderEntity> orderEntityOptional = orderRepository.findById(id);
        if (id <= 0) {
            throw new CustomException(403, "ID hóa đơn phải lớn hơn 0");
        }
        if (orderEntityOptional.isEmpty()) {
            throw new CustomException(403, "Không tìm thấy id hóa đơn muốn hủy");
        }
        OrderEntity order = orderEntityOptional.get();
        order.setStatus("DA_HUY");
        order = orderRepository.save(order);
        return order;
    }

    @Override
    public OrderEntity received(Long id) throws CustomException {
        Optional<OrderEntity> orderEntityOptional = orderRepository.findById(id);
        if (id <= 0) {
            throw new CustomException(403, "ID hóa đơn phải lớn hơn 0");
        }
        if (orderEntityOptional.isEmpty()) {
            throw new CustomException(403, "Không tìm thấy id hóa đơn muốn hủy");
        }
        OrderEntity order = orderEntityOptional.get();
        order.setStatus("DA_NHAN");
        order = orderRepository.save(order);
        return order;
    }

    @Override
    public OrderDetailDTO update(Long id, OrderDetailRequest orderDetailRequest) {
        ModelMapper modelMapper = new ModelMapper();
        Integer idCheck = Math.toIntExact(id);
        OrderDetailDTO orderDTO = new OrderDetailDTO();
        OrderDetailEntity orderDetailEntity = this.orderDetailRepository.getById(id);
        OrderDetailEntity list = this.orderDetailRepository.getOrderDetailByIsCheckAndProductId(idCheck, orderDetailRequest.getProductId());
        if (orderDetailRequest.getIsUpdate() == null) {
            OrderDetailEntity orderDetail = this.orderDetailRepository.getById(orderDetailEntity.getId());
            if (list != null) {
                list.setIsCheck(1);
            }
            orderDetailEntity.setIsCheck(2);
            this.orderDetailRepository.save(orderDetail);
            orderDTO = modelMapper.map(orderDetail, OrderDetailDTO.class);
            orderDetailEntity.setQuantity(orderDetailEntity.getQuantity() - 1);
            if (orderDetailEntity.getQuantity() == 0) {
                orderDetailEntity.setTotal(0);
                orderDetailEntity.setIsCheck(10);
                this.orderDetailRepository.save(orderDetailEntity);
            } else {
                this.orderDetailRepository.save(orderDetailEntity);
                orderDTO = modelMapper.map(orderDetailEntity, OrderDetailDTO.class);
            }
        } else if (orderDetailRequest.getIsUpdate() == 2) {
            orderDetailEntity.setIsCheck(2);

            this.orderDetailRepository.save(orderDetailEntity);
        } else {

            orderDetailEntity.setProduct(orderDetailEntity.getProduct());
            orderDetailEntity.setIsCheck(1);
            this.orderDetailRepository.save(orderDetailEntity);
            orderDTO = modelMapper.map(orderDetailEntity, OrderDetailDTO.class);
        }
        return orderDTO;
    }

    @Override
    public UpdateOrderDetailDTO findByOrderDetailDTO(Long id, UpdateOrderDetailRequest orderDetailRequest) {
        ModelMapper modelMapper = new ModelMapper();
        OrderDetailEntity orderDetailEntity = this.orderDetailRepository.
                getOrderDetailEntityByOrderIsAndAndProduct(id, orderDetailRequest.getProductId());
        if (orderDetailRequest.getQuantity() == orderDetailEntity.getQuantity()) {
            orderDetailEntity.setTotal(0);
        } else if (orderDetailRequest.getQuantity() < orderDetailEntity.getQuantity()) {
            ProductEntity product = this.productRepository.getById(orderDetailRequest.getProductId());
            Double price = product.getPrice();
            int quantityRequest = orderDetailRequest.getQuantity();
            int quantityOD = orderDetailEntity.getQuantity();
            int quantity = quantityOD - quantityRequest;
            Double total = price * quantity;
            orderDetailEntity.setTotal(total);
            orderDetailEntity.setQuantity(orderDetailEntity.getQuantity() - orderDetailRequest.getQuantity());
            product.setQuantity(product.getQuantity() - orderDetailRequest.getQuantity());
            this.productRepository.save(product);
        }
        this.orderDetailRepository.save(orderDetailEntity);
        UpdateOrderDetailDTO orderDTO = modelMapper.map(orderDetailEntity, UpdateOrderDetailDTO.class);
        return orderDTO;
    }

    @Override
    public List<OrderEntity> findUserName(String username) {
        return orderRepository.findUserName(username);
    }

    @Override
    public List<OrderEntity> findPhone(String phone) {
        return orderRepository.findPhone(phone);
    }

    public List<OrderEntity> findByDate(String createdAt) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (createdAt == null || createdAt == "" || createdAt.isEmpty()) {
            return orderRepository.findAll();
        } else {
            return orderRepository.findByDate(LocalDateTime.parse(createdAt, dateTimeFormatter));
        }
    }

    @Override
    public List<OrderDetailEntity> createOrderDetail(List<ExchangeRequest> exchangeRequest) {
        OrderEntity orderEntity = this.orderRepository.getById(exchangeRequest.stream().findFirst().get().getOrderId());
        List<OrderDetailEntity> list = new ArrayList<>();
        exchangeRequest.forEach(exchangeEntity -> {
            OrderDetailEntity orderDetailEntity = new OrderDetailEntity();
            orderDetailEntity.setOrder(orderEntity);
            orderDetailEntity.setQuantity(exchangeEntity.getQuantity());
            orderDetailEntity.setProduct(this.productRepository.getById(exchangeEntity.getProductId()));
            orderDetailEntity.setTotal(this.productRepository.getById(exchangeEntity.getProductId()).getPrice());
            orderDetailEntity.setStatus(OrderDetailStatus.CHO_XAC_NHAN);
            orderDetailEntity.setIsCheck(exchangeEntity.getIsCheck());
            this.orderDetailRepository.save(orderDetailEntity);
            list.add(orderDetailEntity);
        });

        return list;
    }

    @Override
    public OrderDetailDTO updateOrderDetail(Long id, OrderDetailRequest orderDetailRequest) {
        OrderDetailEntity orderDetailEntity = this.orderDetailRepository.getById(id);
        orderDetailEntity.setIsCheck(1);
        this.orderDetailRepository.save(orderDetailEntity);
        ModelMapper modelMapper = new ModelMapper();
        OrderDetailDTO orderDetailDTO = modelMapper.map(orderDetailEntity, OrderDetailDTO.class);
        return orderDetailDTO;
    }

    @Override
    public UpdateOrderDetailDTO updateWithReturn(
            Long orderId, Long orderDetailId,
            UpdateOrderDetailRequest orderDetailRequest
    ) {
        OrderEntity orderEntity = this.orderRepository.getById(orderId);
        OrderDetailEntity orderDetailEntity = this.orderDetailRepository.findByIdAndOrderId(orderDetailId, orderId);
        if (orderDetailEntity != null) {
            int quantity = orderDetailEntity.getQuantity() - orderDetailRequest.getQuantity();
            System.out.println("quantity: " + quantity);
            orderDetailEntity.setQuantity(quantity);
            orderDetailEntity.setTotal(orderDetailEntity.getProduct().getPrice() * quantity);
            this.orderDetailRepository.save(orderDetailEntity);
            System.out.println("số lượng: " + orderDetailEntity.getQuantity());
            System.out.println("tổng tiền: " + orderDetailEntity.getTotal());
            double count = 0;
            List<OrderDetailEntity> list = this.orderDetailRepository.getOrderDetailEntityById(orderId);
            for (OrderDetailEntity od : list) {
                count += od.getTotal();
            }
            System.out.println(count);
            if (count < 0) {
                orderEntity.setTotal(0);
            } else {
                orderEntity.setTotal(count);
            }

            this.orderRepository.save(orderEntity);
        } else {
            throw new RuntimeException("không tìm thấy hoá đơn chi tiết");
        }
        ModelMapper modelMapper = new ModelMapper();
        UpdateOrderDetailDTO orderDetailDTO = modelMapper.map(orderDetailEntity, UpdateOrderDetailDTO.class);
        return orderDetailDTO;
    }

    @Override
    public List<OrderConfirmDTO> findByIdOrderId(List<OrderConfirmDTO> id) {
        id.forEach(orderId -> {
            OrderEntity orderEntity = this.orderRepository.getById(orderId.getId());
            OrderHistoryEntity orderHistory = new OrderHistoryEntity();
            orderEntity.setStatus(orderId.getStatus());
            List<OrderDetailEntity> orderDetailEntity = this.orderDetailRepository.findByOrder(orderEntity.getId());
            if (orderId.getStatus().equals("DA_HUY")) {
                orderDetailEntity.forEach(orderDetailEntity1 -> {
                    ProductEntity product = this.productRepository.getById(orderDetailEntity1.getProduct().getId());
                    product.setQuantity(product.getQuantity() + orderDetailEntity1.getQuantity());
                    this.productRepository.save(product);
                });
                this.orderRepository.save(orderEntity);
            }
            CustomUserDetails authentication1 = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            System.out.println("Tài khoàn đang đăng nhập" + authentication1.getUsername());
            orderHistory.setStatus(String.valueOf(orderEntity.getStatus()));
            orderHistory.setOrderId(orderEntity);
            orderHistory.setTotal(orderEntity.getTotal());
            orderHistory.setVerifier(authentication1.getUsername());
            this.orderHistoryRepository.save(orderHistory);
        });
        return null;
    }

    @Override
    public List<OrderExchangeDTO> updateWhenExchangeCancel(List<OrderExchangeDTO> request, Long orderId) {
        request.forEach(orderExchangeDTO -> {
            OrderDetailEntity orderDetail = this.orderDetailRepository.getById(Long.valueOf(orderExchangeDTO.getIsCheck()));
            orderDetail.setIsCheck(3);
            orderDetail.setTotal(0);
            this.orderDetailRepository.save(orderDetail);
            if(orderExchangeDTO.getIsBoolean().equals("false")){
                ExchangeDetailEntity exchangeDetail = this.exchangeDetailRepository.getByOrderChange(Math.toIntExact(orderDetail.getId()));
                exchangeDetail.setStatus(ReturnDetailStatus.KHONG_XAC_NHAN);
                this.exchangeDetailRepository.save(exchangeDetail);
                ExchangeEntity exchangeEntity = this.exchangeRepository.getById(exchangeDetail.getExchange().getId());
                exchangeEntity.setStatus(ReturnStatus.DA_XU_LY);
                this.exchangeRepository.save(exchangeEntity);
            }
        });
        return null;
    }

    @Override
    public List<OrderExchangeDTO> updateWhenExchange(List<OrderExchangeDTO> request, Long orderId) {


        System.out.println(request.size());
        request.forEach(orderExchangeDTO -> {
            if (orderExchangeDTO.getIsBoolean().equals("true")) {
                System.out.println(orderExchangeDTO.getId());
                Integer Id = Math.toIntExact(orderExchangeDTO.getId());
                System.out.println("isCheck:" + Id);
                System.out.println("productId trong isCheck:" + orderExchangeDTO.getProductId());
                //sản phẩm gửi
                OrderDetailEntity orderDetail = this.orderDetailRepository.getById(Long.valueOf(orderExchangeDTO.getIsCheck()));
                //sản phẩm trước đó
                OrderDetailEntity orderDetailEntity = this.orderDetailRepository.getById(Long.valueOf(orderDetail.getIsCheck()));
                System.out.println("----- sản phẩm trước đó:");
                System.out.println(orderDetailEntity.getId());
                System.out.println(orderDetailEntity.getProduct().getId());
                System.out.println(orderDetailEntity.getQuantity());

                //trừ số lượng sản phẩm nếu đổi hàng thành công
                ProductEntity product = this.productRepository.getById(orderExchangeDTO.getProductId());
                product.setQuantity(product.getQuantity() - orderExchangeDTO.getQuantity());
                this.productRepository.save(product);

                if (orderExchangeDTO.getStatus().equals("1")) {
                    ProductEntity productEntity = this.productRepository.getById(orderDetailEntity.getProduct().getId());
                    productEntity.setQuantity(product.getQuantity() + orderExchangeDTO.getQuantity());
                    this.productRepository.save(productEntity);
                }

                if (orderDetail.getQuantity() == orderDetailEntity.getQuantity()) {
                    System.out.println("------ số lượng sản phảm bằng nhau -------");
                    orderDetailEntity.setTotal(0);
                    orderDetailEntity.setQuantity(0);
                    this.orderDetailRepository.save(orderDetailEntity);
                    orderDetail.setIsCheck(1);
                    this.orderDetailRepository.save(orderDetail);
                } else if (orderDetailEntity.getQuantity() > 0 && orderDetailEntity.getQuantity() > orderDetail.getQuantity()) {
                    int quantity = Integer.valueOf(orderDetailEntity.getQuantity()) - Integer.valueOf(orderDetail.getQuantity());

                    orderDetailEntity.setTotal(
                            orderDetailEntity.getProduct().getPrice() * quantity);
                    System.out.println("Tổng tiền hoá đơn chi tiết: " + (orderDetailEntity.getProduct().getPrice() * (orderDetailEntity.getQuantity() - orderDetail.getQuantity())));
                    orderDetailEntity.setQuantity(orderDetailEntity.getQuantity() - orderDetail.getQuantity());
                    this.orderDetailRepository.save(orderDetailEntity);
                    orderDetail.setIsCheck(1);
                    this.orderDetailRepository.save(orderDetail);
                }
            }
        });
        OrderEntity order = this.orderRepository.getById(orderId);
        double count = 0;
        List<OrderDetailEntity> list = this.orderDetailRepository.findByOrderAndIscheck(order.getId());
        for (OrderDetailEntity od : list) {
            count += od.getTotal();
        }
        System.out.println(count);
        if (count < 0) {
            order.setTotal(0);
        } else {
            order.setTotal(count);
        }
        return null;
    }

    @Override
    public List<StatisticalMonthDTO> statisticalByYear(Integer year) {
        List<StatisticalMonthDTO> list = this.orderRepository.statisticalByYear(year);
        HashMap<Integer, Double> map = new HashMap<>();

        list.forEach((item) -> {
            map.put(item.getMonth(), item.getTotal());
        });
        list.clear();
        for (int i = 1; i <= 12; i++) {
            StatisticalMonthDTO ob = new StatisticalMonthDTO();
            if (map.containsKey(i)) {
                ob.setMonth(i);
                ob.setTotal(map.get(i).doubleValue());
            } else {
                ob.setMonth(i);
                ob.setTotal(0);
            }

            list.add(ob);
        }
        return list;
    }

    @Override
    public List<StatisticalOrderDTO> statisticalByOrder(Integer month, Integer year) {
        List<StatisticalOrderDTO> order = this.orderRepository.statisticalByOrder(month, year);
        System.out.println(order.size());
        return order;
    }

    @Override
    public List<StatisticalProductDTO> statisticalByProduct() {
        List<StatisticalProductDTO> order = this.orderRepository.statisticalByProduct();
        return order;
    }

    @Override
    public SumProductDTO countOrder() {
        SumProductDTO order = this.orderRepository.countOrder();
        return order;
    }

    @Override
    public ImageDTO addImageOrder(ImageOrderRequest request) {
        ImagesEntity imagesEntity = new ImagesEntity();
        imagesEntity.setName(request.getName());
        imagesEntity.setOrder(this.orderRepository.getById(request.getOrder_id()));
        this.imageRepository.save(imagesEntity);
        ModelMapper modelMapper = new ModelMapper();
        ImageDTO imageDTO = modelMapper.map(imagesEntity, ImageDTO.class);
        return imageDTO;
    }

    @Override
    public OrderEntity updateNoteRequest(Long id, UpdateNoteRequest request) {
        OrderEntity order = this.orderRepository.getById(id);
        order.setNote(request.getNote());
        return order;
    }

    @Override
    public SumProductDTO numberOfProductsSold() {
        SumProductDTO order = this.orderRepository.numberOfProductsSold();
        return order;
    }


}
