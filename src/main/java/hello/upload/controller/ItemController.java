package hello.upload.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import hello.upload.domain.Item;
import hello.upload.domain.ItemRepository;
import hello.upload.domain.UploadFile;
import hello.upload.file.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ItemController {

	private final ItemRepository itemRepository;
	private final FileStore fileStore;
	
	@GetMapping("/items/new")
	public String newItem() {
		return "item-form";
	}
	
	@PostMapping("/items/new")
	public String saveItem(@ModelAttribute ItemForm form, RedirectAttributes redirectAttributes) throws IllegalStateException, IOException {
		UploadFile attachFile = fileStore.storeFile(form.getAttachFile());
		List<UploadFile> storeImageFiles = fileStore.storeFiles(form.getImageFiles());
		
		Item item = new Item();
		item.setItemName(form.getItemName());
		item.setAttachFile(attachFile);
		item.setImageFiles(storeImageFiles);
		itemRepository.save(item);
		
		redirectAttributes.addAttribute("itemId", item.getId());
		return "redirect:/items/{itemId}";
	}
	
	@GetMapping("/items/{itemId}")
	public String items(@PathVariable Long itemId, Model model) {
		
		Item item = itemRepository.findById(itemId);
		model.addAttribute("item", item);
		return "item-view";
	}
	
	@ResponseBody
	@GetMapping("/images/{filename}")
	public Resource downloadImage(@PathVariable String filename) throws MalformedURLException {
		return new UrlResource("file:" + fileStore.getFullPath(filename));
	}
	
	@GetMapping("/attach/{itemId}")
	public ResponseEntity<Resource> downloadAttach(@PathVariable Long itemId) throws MalformedURLException{
		Item item = itemRepository.findById(itemId);
		String uploadFileName = item.getAttachFile().getUploadFileName();
		String storeFileName = item.getAttachFile().getStoreFileName();
		
		UrlResource resource = new UrlResource("file:" + fileStore.getFullPath(storeFileName));
		
		String encodeUploadFileName = UriUtils.encode(uploadFileName, StandardCharsets.UTF_8);
		String contentDisposition = "attachment; filename=\"" + encodeUploadFileName +"\"";
		
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
				.body(resource);
	}
	
}
