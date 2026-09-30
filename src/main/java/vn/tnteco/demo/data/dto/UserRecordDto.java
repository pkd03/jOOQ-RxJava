package vn.tnteco.demo.data.dto;

public record UserRecordDto(
        Long id,
        String name,
        String email,
        Integer age,
        String status
) {
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
