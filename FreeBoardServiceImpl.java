package com.study.free.service;

import java.sql.Connection;
import java.util.List;

import javax.inject.Inject;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Service;

import com.study.attach.dao.IAttachDao;
import com.study.attach.vo.AttachVO;
import com.study.exception.BizNotEffectedException;
import com.study.exception.BizNotFoundException;
import com.study.exception.BizPasswordNotMatchedException;
import com.study.free.dao.IFreeBoardDao;
import com.study.free.vo.FreeBoardSearchVO;
import com.study.free.vo.FreeBoardVO;

@Service
public class FreeBoardServiceImpl implements IFreeBoardService {

	@Inject
	IFreeBoardDao freeBoardDao;
	@Inject
	IAttachDao attachDao;
	
	@Override
	public List<FreeBoardVO> getBoardList(FreeBoardSearchVO searchVO) {
		int totalRowCount = freeBoardDao.getTotalRowCount(searchVO);
		searchVO.setTotalRowCount(totalRowCount);
		searchVO.pageSetting();
		return freeBoardDao.getBoardList(searchVO);
	}

	@Override
	public FreeBoardVO getBoard(int boNo) throws BizNotFoundException {
		FreeBoardVO freeBoard = freeBoardDao.getBoard(boNo);
		// freeBoard가 null (글번호를 사용자가 url에 이상하게 입력한 경우 )
		if (freeBoard == null) {
			throw new BizNotFoundException();
		}
		// SELECT KEY
		
		// FreeBoardVO getBoard할 때 freeBoardVO의 필드에 있는 List<AttachVO>도 한번에 setting 되게
		// resultMap을 이용해서 한번에 처리가능..select 할때만 (따로 setAttaches 하지 않아도 된다. 복잡한 join 관계를 쉽게 처리 해준다.)
		
//		List<AttachVO> attaches = attachDao.getAttachListByParent(boNo, "FREE");
//		freeBoard.setAttaches(attaches);
		
		return freeBoard;
	}

	@Override
	public void increaseHit(int boNo) throws BizNotEffectedException {
		int cnt = freeBoardDao.increaseHit(boNo); // update된 행 수가 return 됨
		if (cnt == 0) { // 업데이트가 제대로 안됬다.. 근데 사실 일어날수가 없는 일인데..
			throw new BizNotEffectedException();
		}
	}

	@Override
	public void modifyBoard(FreeBoardVO freeBoard)
			throws BizNotFoundException, BizPasswordNotMatchedException, BizNotEffectedException {
		FreeBoardVO vo = freeBoardDao.getBoard(freeBoard.getBoNo());
		if (vo == null)
			throw new BizNotFoundException();
		if (freeBoard.getBoPass().equals(vo.getBoPass())) {
			int cnt = freeBoardDao.updateBoard(freeBoard);
			if (cnt == 0)
				throw new BizNotEffectedException();
			
			// 추가된 파일들 insert
			List<AttachVO> attaches = freeBoard.getAttaches();
			if (attaches != null) {
				//	list 개수만큼 attachDao.insertAttach;
				for (AttachVO attach : attaches) {
					attach.setAtchParentNo(freeBoard.getBoNo());
					attachDao.insertAttach(attach);
				}
			}
			
			// 삭제할 파일번호들 가지고 삭제 (upload된 실제 파일은 건들지 않습니다.)
			int[] delAtchNos = freeBoard.getDelAtchNos();
			if (delAtchNos != null && delAtchNos.length > 0) {
				attachDao.deleteAttaches(delAtchNos);
			}
			
		} else {
			throw new BizPasswordNotMatchedException();
		}
	}

	@Override
	public void removeBoard(FreeBoardVO freeBoard)
			throws BizNotFoundException, BizPasswordNotMatchedException, BizNotEffectedException {
		FreeBoardVO vo = freeBoardDao.getBoard(freeBoard.getBoNo());
		if (vo == null)
			throw new BizNotFoundException();
		if (freeBoard.getBoPass().equals(vo.getBoPass())) {
			int cnt = freeBoardDao.deleteBoard(freeBoard);
			if (cnt == 0)
				throw new BizNotEffectedException();
		} else {
			throw new BizPasswordNotMatchedException();
		}

	}

	@Override
	public void registBoard(FreeBoardVO freeBoard) throws BizNotEffectedException {
		// freeBoard에는 분명히 attaches도 있다.
		int cnt = freeBoardDao.insertBoard(freeBoard);
		// mybatis에서 seq nextval 한 값이 freeBoard에 세팅 되었으면...
		List<AttachVO> attaches = freeBoard.getAttaches();
		if (attaches != null) {
			//	list 개수만큼 attachDao.insertAttach;
			for (AttachVO attach : attaches) {
				attach.setAtchParentNo(freeBoard.getBoNo());
				attachDao.insertAttach(attach);
			}
		}
		if (cnt == 0)
			throw new BizNotEffectedException();
	}

}
