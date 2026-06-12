package com.example.flexbid.service;

import com.example.flexbid.model.Bid;
import com.example.flexbid.model.Order;
import com.example.flexbid.model.Payment;
import com.example.flexbid.model.Product;
import com.example.flexbid.model.ProductImage;
import com.example.flexbid.model.User;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import jakarta.mail.internet.MimeMessage;
import jakarta.activation.DataSource;
import jakarta.mail.util.ByteArrayDataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendWinnerEmail(User user, Product product, List<ProductImage> images, Bid bid) {
        String to = user.getEmail();
        String subject = "🎉 You won the auction for " + product.getName();

        StringBuilder content = new StringBuilder();
        content.append("<h2>Congratulations, ").append(user.getUsername()).append("!</h2>");
        content.append("<p>You have won the bid for <strong>").append(product.getName()).append("</strong>.</p>");
        content.append("<p><strong>Winning Amount:</strong> ₹").append(bid.getAmount()).append("</p>");
        content.append("<p><strong>Description:</strong> ").append(product.getDescription()).append("</p>");
        content.append("<p><strong>Brand:</strong> ").append(product.getBrand()).append("</p>");
        content.append("<p><strong>Category:</strong> ").append(product.getCategory()).append("</p>");
        content.append("<p><strong>Bidding Round:</strong> ").append(product.getCurrentBiddingRound()).append("</p>");

        if (images != null && !images.isEmpty()) {
            content.append("<h3>Product Images:</h3>");
            for (ProductImage image : images) {
                content.append("<img src='").append(image.getImageUrl())
                        .append("' alt='Product Image' width='400' style='margin:10px 0;' /><br>");
            }
        }

        content.append("<p>Thank you for using <strong>FlexBid</strong>!</p>");

        sendHtmlEmail(to, subject, content.toString());
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);
            System.out.println("📧 Email sent to " + to);
        } catch (Exception e) {
            System.err.println("❌ Failed to send email to " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    public byte[] generateOrderReceiptPdf(Order order, Payment payment) throws Exception {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

        // 🖼️ Optional: Add logo (place logo.png inside src/main/resources)
        try {
            InputStream logoStream = new ClassPathResource("flexbidlogo-removebg-preview.png").getInputStream();
            Image logo = Image.getInstance(logoStream.readAllBytes());
            logo.scaleToFit(150, 100);
            logo.setAlignment(Image.ALIGN_LEFT);
            document.add(logo);
        } catch (Exception e) {
            System.out.println("⚠️ Logo not found, skipping logo in PDF.");
        }

        document.add(new Paragraph("🧾 FLEXBID - Order Receipt", headerFont));
        document.add(new LineSeparator());
        document.add(Chunk.NEWLINE);

        // 📋 Order details
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String formattedDate = order.getCreatedAt().format(formatter);

        document.add(new Paragraph("Order ID: " + order.getId(), normalFont));
        document.add(new Paragraph("Product: " + order.getProduct().getName(), normalFont));
        document.add(new Paragraph("Buyer: " + order.getBillingName(), normalFont));
        document.add(new Paragraph("Amount Paid: ₹" + order.getTotalAmount(), boldFont));
        document.add(new Paragraph("Payment ID: " + payment.getRazorpayPaymentId(), normalFont));
        document.add(new Paragraph("Contact: " + order.getContactNumber(), normalFont));
        document.add(new Paragraph("Address: " + order.getAddress(), normalFont));
        document.add(new Paragraph("Date: " + formattedDate, normalFont));

        document.add(Chunk.NEWLINE);
        document.add(new LineSeparator());
        document.add(new Paragraph("Thank you for using FlexBid!", normalFont));
        document.close();

        return out.toByteArray();
    }

    public void sendOrderSuccessEmail(Order order, Payment payment) {
        String to = order.getUser().getEmail();
        String subject = "✅ Order Confirmed: " + order.getProduct().getName();

        StringBuilder content = new StringBuilder();
        content.append("<h2>Hi ").append(order.getBillingName()).append(",</h2>");
        content.append("<p>Your order has been successfully confirmed!</p>");
        content.append("<p><strong>Order ID:</strong> ").append(order.getId()).append("</p>");
        content.append("<p><strong>Amount Paid:</strong> ₹").append(order.getTotalAmount()).append("</p>");
        content.append("<p>The receipt is attached in PDF format for your records.</p>");
        content.append("<br>");
        content.append("<p>Thanks for using <strong>FlexBid</strong>!</p>");
        content.append("<hr>");
        content.append("<p style='color:#555;font-size:12px;'>If you have any questions, please contact <a href='mailto:support@flexbid.com'>support@flexbid.com</a></p>");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content.toString(), true); // HTML email

            // 📎 Attach PDF
            byte[] pdfBytes = generateOrderReceiptPdf(order, payment);
            DataSource dataSource = new ByteArrayDataSource(pdfBytes, "application/pdf");
            helper.addAttachment("FlexBid_Receipt_Order_" + order.getId() + ".pdf", dataSource);

            mailSender.send(message);
            System.out.println("📧 Order confirmation with PDF sent to " + to);
        } catch (Exception e) {
            System.err.println("❌ Failed to send PDF receipt: " + e.getMessage());
        }
    }

    
}
